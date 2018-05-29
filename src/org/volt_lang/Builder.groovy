package org.volt_lang

import org.volt_lang.RepoConf
import org.volt_lang.NodeConf
import org.volt_lang.ToolchainConf
import static org.volt_lang.Helpers.*


class Builder implements Serializable
{
	/// The dsl object that allows us to use steps.
	def dsl

	/// The scm object if any.
	def scm

	/// The environment.
	def env

	/// Are we doing a full build aka Volta.
	def isVolta

	/// Configuration of repos.
	def repoConfs = [
		new RepoConf(name: 'amp',      lib: true,  gitUrl: 'https://github.com/VoltLang/Amp'       ),
		new RepoConf(name: 'watt',     lib: true,  gitUrl: 'https://github.com/VoltLang/Watt'      ),
		new RepoConf(name: 'tesla',    lib: false, gitUrl: 'https://github.com/VoltLang/Tesla'     ),
		new RepoConf(name: 'volta',    lib: false, gitUrl: 'https://github.com/VoltLang/Volta'     ),
		new RepoConf(name: 'diode',    lib: false, gitUrl: 'https://github.com/VoltLang/Diode'     ),
		new RepoConf(name: 'charge',   lib: false, gitUrl: 'https://github.com/VoltLang/Charge'    ),
		new RepoConf(name: 'battery',  lib: false, gitUrl: 'https://github.com/VoltLang/Battery'   ),
		new RepoConf(name: 'fourier',  lib: false, gitUrl: 'https://github.com/VoltLang/Fourier'   ),
		new RepoConf(name: 'injiki',   lib: false, gitUrl: 'https://github.com/bhelyer/Injiki'     ),
		new RepoConf(name: 'quickhex', lib: false, gitUrl: 'https://github.com/Wallbraker/QuickHex'),
	]

	/// Which targets to build and on which nodes.
	def nodeConfs = [
		new NodeConf(arch: 'x86',    plat: 'linux', cross: false, node: 'ubuntu-16.04-i386'  ),
		new NodeConf(arch: 'x86_64', plat: 'linux', cross: false, node: 'ubuntu-16.04-x86_64'),
		new NodeConf(arch: 'x86_64', plat: 'osx',   cross: false, node: 'macosx-10.11-x86_64'),
		new NodeConf(arch: 'x86_64', plat: 'msvc',  cross: true,  node: 'ubuntu-16.04-x86_64'),
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
			conf.gitUrl = null
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

	def setupToolchain(ToolchainConf toolchainConf)
	{
		def watt = findRepoConf('watt')
		def volta = findRepoConf('volta')
		def battery = findRepoConf('battery')

		watt.gitTag = toolchainConf.wattTag
		volta.gitTag = toolchainConf.voltaTag
		battery.gitTag = toolchainConf.batteryTag

		this.repoConfs = [watt, volta, battery]

		dsl.parallel makeSetup()
		dsl.echo makeStr()
	}

	def addToolchainLib(folder)
	{
		for (repoConf in repoConfs) {
			if (repoConf.name == folder) {
				return;
			}
		}

		def repoConf = new RepoConf(name: folder, lib: true);
		repoConf.toolchain = true
		repoConfs.push(repoConf)
	}

	def doSetupProject(folder, lib)
	{
		def repoConf = getOrAddScmRepoConf(folder)
		repoConf.lib = lib
		if (scm != null) {
			repoConf.gitUrl = null
		}

		repoConfs = [repoConf]
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
		for (nodeConf in nodeConfs) {
			unique[nodeConf.node] = []
		}
		for (nodeConf in nodeConfs) {
			unique[nodeConf.node].push(nodeConf)
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
					for (nodeConf in arr) {
						nodeConf.dir = d
					}
				}
			}
		}
		return branches
	}

	def makeStr()
	{
		def ret = "Target and node config:\n"
		for (nodeConf in nodeConfs) {
			ret = "${ret}\ttarget: \'${nodeConf.arch}-${nodeConf.plat}\'\n"
			ret = "${ret}\t\tnode: \'${nodeConf.node}\'\n"
			ret = "${ret}\t\tdir: \'${nodeConf.dir}\'\n"
			ret = "${ret}\t\tcross: \'${nodeConf.cross}\'\n"
		}

		ret = "${ret}\nRepo configs:\n"
		for (repoConf in repoConfs) {
			ret = "${ret}\tname: \'${repoConf.name}\'\n"

			if (repoConf.gitUrl != null) {
				ret = "${ret}\t\tgitUrl: \'${repoConf.gitUrl}\'\n"
			} else if (repoConf.toolchain) {
				ret = "${ret}\t\ttoolchain: \'true\'\n"
			} else if (scm != null) {
				ret = "${ret}\t\tscm: \'true\'\n"
			} else {
				ret = "${ret}\t\terror: \'true\'\n"
			}

			if (repoConf.gitTag != null) {
				ret = "${ret}\t\ttag: \'${repoConf.gitTag}\'\n"
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
			def repoConf = c
			if (repoConf.toolchain) {
				continue;
			}

			setTag(repoConf)

			branches[repoConf.name] = {
				doCheckout(repoConf)
			}
		}

		return branches
	}

	def doCheckout(repoConf)
	{
		if (repoConf.gitUrl == null) {
			dsl.dir(repoConf.name) {
				dsl.checkout scm
				dsl.stash includes: '**', name: repoConf.stashName
			}
		} else if (repoConf.gitTag) {
			dsl.dir(repoConf.name) {
				dsl.checkout(
					scm: [$class: 'GitSCM',
						userRemoteConfigs: [[url: "${repoConf.gitUrl}"]],
						branches: [[name: "refs/tags/${repoConf.gitTag}"]]
						],
					poll: false,
					changelog: false,
				)
				dsl.stash includes: '**', name: repoConf.stashName
			}
		} else {
			dsl.dir(repoConf.name) {
				dsl.git branch: 'master', changelog: true, poll: true, url: repoConf.gitUrl
				dsl.stash includes: '**', name: repoConf.stashName
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
			def repoConf = c

			def dst = "${dir}/src/${repoConf.name}"
			def src = "${dir}/toolchain/lib/${repoConf.name}"

			// Should we grab this repo from the toolchain or tag.
			if (repoConf.toolchain) {
				dsl.dir("${dir}/src") {
					dsl.sh "mv ${src} ${dst}"
				}
			} else {
				dsl.dir(dst) {
					dsl.unstash(repoConf.stashName)
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
		for (repoConf in repoConfs) {
			ret = "${ret} src/${repoConf.name}"
			if (!repoConf.lib) {
				ret = "${ret} -o ${repoConf.name}"
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
			for (repoConf in repoConfs) {
				if (repoConf.lib) {
					continue;
				}

				def srcFile = "${repoConf.name}${fileSuffix}"
				def tarFile = "${repoConf.name}-${arch}-${plat}.${sufix}"
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
		for (repoConf in repoConfs) {
			if (repoConf.toolchain) {
				continue;
			}
			args = "${args} ${repoConf.name}"
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

		for (nodeConf in nodeConfs) {
			if (nodeConf.cross && !cross) {
				continue;
			}

			// Get the correct values in the delegate
			def tripple = makeTripple(nodeConf)
			def dir = nodeConf.dir
			def node = nodeConf.node
			def arch = nodeConf.arch
			def plat = nodeConf.plat

			branches[tripple] = {
				dsl.node(node) {
					func(dir, arch, plat, arg)
				}
			}
		}

		return branches
	}

	def setTag(repoConf)
	{
		repoConf.stashName = "${repoConf.name}-repo"
		repoConf.toolchain = false
	}

	def getOrAddScmRepoConf(folder)
	{
		for (repoConf in repoConfs) {
			if (repoConf.name == folder) {
				return repoConf
			}
		}

		dsl.echo "Make repo ${folder}"

		def repoConf = new RepoConf(name: folder)
		repoConfs.push(repoConf)
		return repoConf
	}

	def findRepoConf(String name)
	{
		for (repoConf in repoConfs) {
			if (repoConf.name == name) {
				return repoConf
			}
		}
	}
}
