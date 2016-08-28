
/*
 *
 * Main build
 *
 */

def repos = [
	'https://github.com/VoltLang/Amp',
	'https://github.com/VoltLang/Watt',
	'https://github.com/VoltLang/Volta',
	'https://github.com/VoltLang/Tesla',
	'https://github.com/VoltLang/Diode',
	'https://github.com/VoltLang/Charge',
	'https://github.com/VoltLang/Parsec',
	'https://github.com/VoltLang/Battery',
	'https://github.com/VoltLang/Fourier',
	'https://github.com/bhelyer/Injiki',
]

def nodes = [
	'ubuntu-16.04-i386',
	'ubuntu-16.04-x86_64',
	'macosx-10.11-x86_64',
]

stage 'Checkout'
node('master') { checkout(repos) }

stage 'Distribute'
distribute(nodes, this.&buildSources, repos)

stage 'Copy'
distribute(nodes, this.&copyAll, null)

stage 'Build'
distribute(nodes, this.&buildAll, null)

stage 'Test'
distribute(nodes, this.&testAll, null)

stage 'Archive'
distribute(nodes, this.&archiveAll, null)



/*
 *
 * Sub functions
 *
 */

def nameFromRepo(repo)
{
	return repo.substring(repo.lastIndexOf("/") + 1)
}

def dirFromRepo(repo)
{
	return nameFromRepo(repo).toLowerCase()
}

def tagFromRepo(repo)
{
	return "${dirFromRepo(repo)}-repo"
}

def checkout(repos)
{
	def branches = [:]
	for (r in repos) {
		def repo = r
		def name = nameFromRepo(repo)
		branches[name] = {
			checkoutAndStash(repo)
		}
	}
	parallel branches
}

/*
 * Helper function for checkout.
 */
def checkoutAndStash(repo)
{
	def tag = tagFromRepo(repo)
	dir(tag) {
		git poll: false, url: repo
		stash includes: "**", name: tag
	}
}

def distribute(nodes, func, arg)
{
/*
	def branches = [:]
	for (n in nodes) {
		def rebind = n
		branches[rebind] = {
			node(rebind) {
				func(arg)
			}
		}
	}
	parallel branches
*/
	parallel 'ubuntu-16.04-x86_64':{
		node('ubuntu-16.04-x86_64') { func('x86_64-linux', arg) }
	}, 'ubuntu-16.04-i386': {
		node('ubuntu-16.04-i386') { func('x86-linux', arg) }
	}, 'macosx-10.11-x86_64':{
		node('macosx-10.11-x86_64') { func('x86_64-osx', arg) }
	}
}

def copyAll(name, arg)
{
	def file = "battery-${name}.tar.gz"
	dir('bin') {
		deleteDir()
		step([$class: 'CopyArtifact', filter: file, fingerprintArtifacts: true, projectName: 'Binaries'])
		sh """
		tar xfv ${file}
		"""
	}
}

def buildSources(name, repos)
{
	for (repo in repos) {
		dir(dirFromRepo(repo)) {
			deleteDir()
			unstash(tagFromRepo(repo))
		}
	}
}

// Build binaries.
def buildAll(name, arg)
{
	sh """
	rm -rf .bin
	make -C volta volt
	bin/battery config \
		--cmd-volta volta/volt \
		volta \
		amp \
		watt \
		tesla \
		diode \
		injiki \
		charge \
		parsec \
		fourier \
		battery
	bin/battery build
	"""
}

// Run test suit.
def testAll(name, arg)
{
	sh """
	battery/battery test
	"""

	junit 'results.xml'
}

// Archive is destructive, run it last.
def archiveAll(name, arg)
{
	dir('battery') {
		def tarFile = "battery-${name}.tar.gz"
		sh """
		tar -czf ${tarFile} battery
		"""
		archiveArtifacts artifacts: tarFile, fingerprint: true, onlyIfSuccessful: true
	}
	dir('toolchain') {
		def tarFile = "toolchain-${name}.tar.gz"
		deleteDir()
		sh """
		mkdir -p bin
		mkdir -p lib
		mv ../volta/volta bin
		mv ../bin/battery bin
		mv ../volta/rt lib
		mv ../watt lib
		mv ../amp lib
		mv ../parsec lib
		rm -rf lib/rt/test
		rm -rf lib/watt/test
		rm -rf lib/amp/test
		rm -rf lib/parsec/test
		tar -czf ${tarFile} *
		"""
		archiveArtifacts artifacts: tarFile, fingerprint: true, onlyIfSuccessful: true
	}
}
