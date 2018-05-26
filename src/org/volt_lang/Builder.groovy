package org.volt_lang

import org.volt_lang.RepoConf
import org.volt_lang.NodeConf
import org.volt_lang.ToolchainConf


class Builder implements Serializable
{
	/// The dsl object that allows us to use steps.
	def dsl

	/// The scm object if any.
	def scm

	/// The environment
	def env

	/// Are we doing a full build aka Volta
	def isVolta

	/// Configuration of repos.
	def repoConfs = [
		new RepoConf('amp',      'https://github.com/VoltLang/Amp',        true ),
		new RepoConf('watt',     'https://github.com/VoltLang/Watt',       true ),
		new RepoConf('tesla',    'https://github.com/VoltLang/Tesla',      false),
		new RepoConf('volta',    'https://github.com/VoltLang/Volta',      false),
		new RepoConf('diode',    'https://github.com/VoltLang/Diode',      false),
		new RepoConf('charge',   'https://github.com/VoltLang/Charge',     false),
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

	Builder(dsl, scm, env)
	{
		this.dsl = dsl
		this.scm = scm
		this.env = env
	}


	/*
	 *
	 * Setup
	 *
	 */

	def setupExe(folder)
	{
		doSetupProject(folder, false)
	}

	def setupLib(folder)
	{
		doSetupProject(folder, true)
	}

	def setupVolta()
	{
		def conf = getOrAddScmRepoConf('volta')
		if (scm != null) {
			conf.url = null
		}

		isVolta = true

		dsl.parallel makeSetup()
		dsl.echo makeStr()
	}

	def setupGuru(newConfs)
	{
		repoConfs = newConfs

		dsl.parallel makeSetup()
		dsl.echo makeStr()
	}

	def setupToolchain(ToolchainConf conf)
	{
	}

	def addToolchainLib(folder)
	{
		for (repo in repoConfs) {
			if (repo.folder == folder) {
				return;
			}
		}

		def conf = new RepoConf(folder, null, true);
		conf.toolchain = true
		repoConfs.push(conf)
	}

	def doSetupProject(folder, lib)
	{
		def conf = getOrAddScmRepoConf(folder)
		conf.lib = lib
		if (scm != null) {
			conf.url = null
		}

		repoConfs = [conf]
		addToolchainLib('rt')
		addToolchainLib('amp')
		addToolchainLib('watt')

		dsl.parallel makeSetup()
		dsl.echo makeStr()
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
		def ret = "Target and node config:\n"
		for (conf in nodeConfs) {
			ret = "${ret}\ttarget: \'${conf.tag}\'\n"
			ret = "${ret}\t\tnode: \'${conf.node}\'\n"
			ret = "${ret}\t\tdir: \'${conf.dir}\'\n"
		}

		ret = "${ret}\nRepo configs:\n"
		for (conf in repoConfs) {
			ret = "${ret}\tfolder: \'${conf.folder}\'\n"

			if (conf.url != null) {
				ret = "${ret}\t\turl: \'${conf.url}\'\n"
			} else if (conf.toolchain) {
				ret = "${ret}\t\ttoolchain: \'true\'\n"
			} else if (scm != null) {
				ret = "${ret}\t\tscm: \'true\'\n"
			} else {
				ret = "${ret}\t\terror: \'true\'\n"
			}
		}

		return ret
	}


	/*
	 *
	 * Checkout
	 *
	 */

	def checkout()
	{
		dsl.node('master') {
			dsl.parallel makeCheckout()
		}
	}

	def makeCheckout()
	{
		def branches = [:]

		for (c in repoConfs) {
			def conf = c
			if (conf.toolchain) {
				continue;
			}

			setTag(conf)

			branches[conf.folder] = {
				doCheckout(conf)
			}
		}

		return branches
	}

	def doCheckout(conf)
	{
		if (conf.url == null) {
			dsl.dir(conf.folder) {
				dsl.checkout scm
				dsl.stash includes: '**', name: conf.tag
			}
		} else {
			dsl.dir(conf.folder) {
				dsl.git branch: 'master', changelog: true, poll: true, url: conf.url
				dsl.stash includes: '**', name: conf.tag
			}
		}
	}


	/*
	 *
	 * Setting up for the build.
	 *
	 */

	def prepare()
	{
		if (isVolta) {
			dispatch(this.&doPrepareVolta, false, null)
		} else {
			dispatch(this.&doPrepare, false, null)
		}
	}

	def doPrepare(dir, arch, plat, arg)
	{
		doToolchain(dir, arch, plat, arg);
		doSources(dir, arch, plat, arg);
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

			def dst = "${dir}/src/${conf.folder}"
			def src = "${dir}/toolchain/lib/${conf.folder}"

			// Should we grab this repo from the toolchain or tag.
			if (conf.toolchain) {
				dsl.dir("${dir}/src") {
					dsl.sh "mv ${src} ${dst}"
				}
			} else {
				dsl.dir(dst) {
					dsl.unstash(conf.tag)
				}
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

		dsl.dir("${dir}/toolchain") {
			dsl.deleteDir()
			dsl.step([$class: 'CopyArtifact', filter: file, fingerprintArtifacts: true, projectName: 'Volt'])
			dsl.sh """
			tar xf ${file}
			rm ${file}
			rm -rf ../bin
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
		def volta = "${dir}/bin/volta"
		def battery = "${dir}/bin/battery"

		dsl.dir("${dir}/bin") {
			dsl.step([$class: 'CopyArtifact', filter: file, fingerprintArtifacts: true, projectName: 'Binaries'])
			dsl.sh """
			tar xf ${file}
			rm ${file}

			make -C ../src/volta TARGET=${volta} ${volta}

			strip ${volta}
			strip ${battery}
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
		def cmd = plat == 'msvc' ? 'zip -q' : 'tar -czf'
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
		zip -q -r sources.zip ${args} -x "*/.git/*"
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
		conf.toolchain = false
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
