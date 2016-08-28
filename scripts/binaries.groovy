// This script downloads the latest release from Battery and archive them.

import groovy.json.JsonSlurper


stage 'Download'
node('master') {
	def tmpFile = 'temp.json'
	def url = "https://api.github.com/repos/VoltLang/Battery/releases/latest"
	sh """
	curl ${url} -o ${tmpFile}
	"""

	def text = readFile tmpFile
	def commands = getCommands(text)
	def files = getFiles(text)

	for (cmd in commands) {
		sh cmd
	}

	for (file in files) {
		archiveArtifacts file
	}
}

def getCommands(text)
{
	def root = (new JsonSlurper()).parseText(text)
	def r = "${root.tag_name.substring(1)}-"
	def files = []

	for (a in root.assets) {
		def dst = a.name.replace(r, '')
		files.add("""
		curl -L ${a.browser_download_url} -o ${dst}
		""")
		a = null
	}
	root = null
	return files
}

def getFiles(text)
{
	def root = (new JsonSlurper()).parseText(text)
	def r = "${root.tag_name.substring(1)}-"
	def files = []

	for (a in root.assets) {
		def dst = a.name.replace(r, '')
		files.add("${dst}")
	}
	root = null
	return files
}
