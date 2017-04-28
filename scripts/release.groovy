stage ('Copy')
node('master') {
	def tag = ask()
	def from = [
		'battery-x86-linux.tar.gz',
		'battery-x86_64-linux.tar.gz',
		'battery-x86_64-osx.tar.gz',
		'battery-x86_64-msvc.zip',
	]
	def to = [
		"battery-${tag}-x86-linux.tar.gz",
		"battery-${tag}-x86_64-linux.tar.gz",
		"battery-${tag}-x86_64-osx.tar.gz",
		"battery-${tag}-x86_64-msvc.zip",
	]

	for (file in from) {
		step([$class: 'CopyArtifact',
		     filter: file,
		     fingerprintArtifacts: true,
		     projectName: 'Volt'])
	}

	sh """
	mv ${from[0]} ${to[0]}
	mv ${from[1]} ${to[1]}
	mv ${from[2]} ${to[2]}
	mv ${from[3]} ${to[3]}
	"""

	for (file in to) {
		archiveArtifacts(artifacts: file, fingerprint: true)
	}
}

def ask()
{
	def q = 'Please enter release version'
	def desc = 'Version name for the release'
	def ret = input message: q, ok: 'Ok', parameters: [string(defaultValue: '0.0.0', description: desc, name: 'Version')]
	return ret
}
