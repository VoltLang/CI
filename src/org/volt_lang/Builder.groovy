package org.volt_lang

import org.volt_lang.RepoConf
import org.volt_lang.NodeConf


class Builder implements Serializable
{
	/// The dsl object that allows us to use steps.
	def dsl

	/// Configuration of repos.
	def repoConfs = [
		new RepoConf('amp',      'https://github.com/VoltLang/Amp',        true ),
		new RepoConf('watt',     'https://github.com/VoltLang/Watt',       true ),
		new RepoConf('tesla',    'https://github.com/VoltLang/Tesla',      false),
		new RepoConf('volta',    'https://github.com/VoltLang/Volta',      false),
		new RepoConf('diode',    'https://github.com/VoltLang/Diode',      false),
		new RepoConf('charge',   'https://github.com/VoltLang/Charge',     false),
		new RepoConf('parsec',   'https://github.com/VoltLang/Parsec',     true ),
		new RepoConf('battery',  'https://github.com/VoltLang/Battery',    false),
		new RepoConf('fourier',  'https://github.com/VoltLang/Fourier',    false),
		new RepoConf('injiki',   'https://github.com/bhelyer/Injiki',      false),
		new RepoConf('quickhex', 'https://github.com/Wallbraker/QuickHex', false),
	]

	/// Which targets to build and on which nodes.
	def nodeConfs = [
		new NodeConf('x86',    'linux', false, 'ubuntu-16.04-i386'  ),
		new NodeConf('x86_64', 'linux', false, 'ubuntu-16.04-x86_64'),
		new NodeConf('x86_64', 'osx',   false, 'macosx-10.11-x86_64'),
		new NodeConf('x86_64', 'msvc',  true,  'ubuntu-16.04-x86_64'),
	]

	Builder(dsl)
	{
		this.dsl = dsl
	}


	/*
	 *
	 * Setup
	 *
	 */

	def setup()
	{
		dsl.parallel makeSetup()
		dsl.echo makeStr()
	}

	def replaceRepos(newConfs)
	{
		repoConfs = newConfs
	}

	def addRepos(newConfs)
	{
		repoConfs += newConfs
	}

	def doSort()
	{
		def sorted = []
		def unique = [:]
		for (conf in nodeConfs) {
			unique[conf.node] = []
		}
		for (conf in nodeConfs) {
			unique[conf.node].push(conf)
		}
		for (e in unique) {
			sorted.push(e.value)
		}
		return sorted
	}

	def makeSetup()
	{
		def sorted = doSort()
		def branches = [:]
		for (a in sorted) {
			def arr = a
			def node = arr[0].node
			branches[node] = {
				dsl.node(node) {
					def d = dsl.pwd()
					for (conf in arr) {
						conf.dir = d
					}
				}
			}
		}
		return branches
	}

	def makeStr()
	{
		def ret = "Target and node config:"
		for (conf in nodeConfs) {
			ret = "${ret}\ntarget: \'${conf.tag}\'\n"
			ret = "${ret}\tnode:\'${conf.node}\'\n"
			ret = "${ret}\tdir:\'${conf.dir}\'\n"
		}

		return ret
	}


	/*
	 *
	 * Checkout
	 *
	 */

	def checkoutAll()
	{
		dsl.node('master') {
			dsl.parallel makeCheckout(null, null)
		}
	}

	def checkoutSCM(folder, scm)
	{
		def conf = getOrAddScmRepoConf(folder)
		dsl.node('master') {
			setTag(conf)
			dsl.dir(conf.folder) {
				dsl.checkout scm
				dsl.stash includes: '**', name: conf.tag
			}
		}

		repoConfs = [
			conf,
			new RepoConf('rt',   null, true),
			new RepoConf('amp',  null, true),
			new RepoConf('watt', null, true),
		]

		repoConfs[1].toolchain = true
		repoConfs[2].toolchain = true
		repoConfs[3].toolchain = true
	}

	def checkoutVoltaSCM(scm)
	{
		dsl.node('master') {
			dsl.parallel makeCheckout('volta', scm)
		}
	}

	def makeCheckout(folder, scm)
	{
		def branches = [:]

		if (folder != null) {
			def conf = getOrAddScmRepoConf(folder)
			setTag(conf)

			branches[conf.folder] = {
				dsl.dir(conf.folder) {
					dsl.checkout scm
					dsl.stash includes: '**', name: conf.tag
				}
			}
		}

		for (c in repoConfs) {
			def conf = c
			setTag(conf)

			if (conf.folder == folder) {
				continue;
			}

			branches[conf.folder] = {
				dsl.dir(conf.folder) {
					dsl.git branch: 'master', changelog: true, poll: true, url: conf.url
					dsl.stash includes: '**', name: conf.tag
				}
			}
		}

		return branches
	}


	/*
	 *
	 * Setting up for the build.
	 *
	 */

	def prepare()
	{
		dispatch(this.&doPrepare, false, null)
	}

	def prepareVolta()
	{
		dispatch(this.&doPrepareVolta, false, null)
	}

	def doPrepare(dir, arch, plat, arg)
	{
		doSources(dir, arch, plat, arg);
		doToolchain(dir, arch, plat, arg);
	}

	def doPrepareVolta(dir, arch, plat, arg)
	{
		doSources(dir, arch, plat, arg);
		doBootstrap(dir, arch, plat, arg);
	}


	/*
	 *
	 * Distrobute the source
	 *
	 */

	def doSources(dir, arch, plat, arg)
	{
		dsl.dir("${dir}/src") {
			dsl.deleteDir()
		}

		for (c in repoConfs) {
			def conf = c

			// If a repo doesn't have a tag skip it.
			if (conf.tag == null) {
				continue;
			}

			dsl.dir("${dir}/src/${conf.folder}") {
				dsl.unstash(conf.tag)
			}
		}
	}


	/*
	 *
	 * Toolchain
	 *
	 */

	def doToolchain(dir, arch, plat, arg)
	{
		def file = "toolchain-${arch}-${plat}.tar.gz"
		dsl.dir("${dir}/bin") {
			dsl.deleteDir()
		}
		dsl.dir("${dir}/toolchain") {
			dsl.deleteDir()
			dsl.step([$class: 'CopyArtifact', filter: file, fingerprintArtifacts: true, projectName: 'Volt'])
			dsl.sh """
			tar xfv ${file}
			rm ${file}
			mv lib/* ../src
			mv bin ..
			"""
		}
	}


	/*
	 *
	 * Bootstrap
	 *
	 */

	def doBootstrap(dir, arch, plat, arg)
	{
		def file = "battery-${arch}-${plat}.tar.gz"
		dsl.dir("${dir}/bin") {
			dsl.deleteDir()
			dsl.step([$class: 'CopyArtifact', filter: file, fingerprintArtifacts: true, projectName: 'Binaries'])
			dsl.sh """
			tar xfv ${file}
			rm ${file}
			"""
		}

		dsl.dir(dir) {
			dsl.sh """
			make -C src/volta volt
			mv src/volta/volt bin/volta
			strip bin/volta
			strip bin/battery
			"""
		}
	}


	/*
	 *
	 * Building
	 *
	 */

	def build()
	{
		dispatch(this.&doBuild, true, null)
	}

	def doBuild(dir, arch, plat, arg)
	{
		dir = "${dir}/${arch}-${plat}"
		dsl.dir(dir) {
			dsl.sh """
			rsync -r -v --checksum --delete ../src/ src
			rsync -r -v --checksum --delete ../bin/ bin
			bin/battery config \
				--arch ${arch} \
				--platform ${plat} \
				--cmd-volta bin/volta \
				${makeConfigStr()}
			bin/battery build
			"""
		}
	}

	def makeConfigStr(prefix)
	{
		def ret = ""
		for (conf in repoConfs) {
			ret = "${ret} src/${conf.folder}"
			if (!conf.lib) {
				ret = "${ret} -o ${conf.folder}"
			}
		}
		return ret
	}


	/*
	 *
	 * Testing
	 *
	 */

	def test()
	{
		dispatch(this.&doTest, false, null)
	}

	def doTest(dir, arch, plat, arg)
	{
		dir = "${dir}/${arch}-${plat}"
		dsl.dir(dir) {
			dsl.sh """
			bin/battery test
			"""

			// For now, improve battery instead.
			if (dsl.fileExists('results.xml')) {
				dsl.junit 'results.xml'
			}
		}
	}


	/*
	 *
	 * Archive
	 *
	 */

	def archive()
	{
		dispatchWithMaster(this.&doArchive, true, this.&doArchiveSources, null)
	}

	def doArchive(dir, arch, plat, arg)
	{
		dir = "${dir}/${arch}-${plat}"

		// Use zip for msvc or tar for nix.
		def cmd = plat == 'msvc' ? 'zip' : 'tar -czf'
		def sufix = plat == 'msvc' ? 'zip' : 'tar.gz'
		def fileSuffix = plat == 'msvc' ? '.exe' : ''

		dsl.dir(dir) {
			for (conf in repoConfs) {
				if (conf.lib) {
					continue;
				}

				def srcFile = "${conf.folder}${fileSuffix}"
				def tarFile = "${conf.folder}-${arch}-${plat}.${sufix}"
				dsl.sh """
				rm -f ${tarFile}
				${cmd} ${tarFile} ${srcFile}
				"""
				dsl.archiveArtifacts artifacts: tarFile, fingerprint: true, onlyIfSuccessful: true
			}
		}
	}

	def doArchiveSources(dir, arch, plat, arg)
	{
		def zipFile = 'sources.zip'
		def tarFile = 'sources.tar.gz'
		def args = ""
		for (conf in repoConfs) {
			if (conf.toolchain) {
				continue;
			}
			args = "${args} ${conf.folder}"
		}
		dsl.sh """
		rm -f sources.zip source.tar.gz
		zip -r sources.zip ${args} -x "*/.git/*"
		tar -czf sources.tar.gz --exclude="*/.git/*" ${args}
		"""
		dsl.archiveArtifacts artifacts: tarFile, fingerprint: true
		dsl.archiveArtifacts artifacts: zipFile, fingerprint: true
	}


	/*
	 *
	 * Helper functions.
	 *
	 */

	def dispatch(func, cross, arg)
	{
		dsl.parallel makeDispatch(func, cross, null, arg)
	}

	def dispatchWithMaster(func, cross, funcMaster, arg)
	{
		dsl.parallel makeDispatch(func, cross, funcMaster, arg)
	}

	def makeDispatch(func, cross, funcMaster, arg)
	{
		def branches = [:]

		if (funcMaster != null) {
			branches['master'] = {
				dsl.node('master') {
					funcMaster(null, null, null, arg)
				}
			}
		}

		for (c in nodeConfs) {
			def conf = c

			if (conf.cross && !cross) {
				continue;
			}

			branches[conf.tag] = {
				dsl.node(conf.node) {
					func(conf.dir, conf.arch, conf.plat, arg)
				}
			}
		}
		return branches
	}

	def setTag(conf)
	{
		conf.tag = "${conf.folder}-repo"
	}

	def getOrAddScmRepoConf(folder)
	{
		for (conf in repoConfs) {
			if (conf.folder == folder) {
				return conf
			}
		}

		dsl.echo "Make repo ${folder}"

		def conf = new RepoConf(folder, null, false)
		repoConfs.push(conf)
		return conf
	}
}
