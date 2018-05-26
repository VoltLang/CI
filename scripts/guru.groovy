@Library('volt-build')
import org.volt_lang.Builder
import org.volt_lang.RepoConf

def b = new Builder(steps, null, env)

stage('Setup') {
	b.setupGuru([
		new RepoConf('guru', 'https://github.com/VoltLang/Guru', false),
		new RepoConf('metal', 'https://github.com/VoltLang/Metal', false)
	])
}

stage('Checkout') {
	b.checkout()
}

stage('Toolchain') {
	def fileTool = "toolchain-x86_64-linux.tar.gz"
	def fileDiode = "diode-x86_64-linux.tar.gz"
	def fileSources = "sources.tar.gz"

	node('master') {
		dir('staging') {
			deleteDir()
			step([$class: 'CopyArtifact', filter: fileTool, fingerprintArtifacts: true, projectName: 'Volt'])
			step([$class: 'CopyArtifact', filter: fileDiode, fingerprintArtifacts: true, projectName: 'Volt'])
			step([$class: 'CopyArtifact', filter: fileSources, fingerprintArtifacts: true, projectName: 'Volt'])
			sh """
			tar xf ${fileTool}
			tar xf ${fileDiode}
			strip diode
			mv diode bin/diode
			tar xf ${fileSources}
			rm ${fileTool} ${fileDiode} ${fileSources}
			rm -rf lib
			rsync -r -v --checksum --delete --exclude ".git" ../guru/ guru
			rsync -r -v --checksum --delete --exclude ".git" ../metal/ metal
			"""
		}
	}
}

stage('Generate') {
	node('master') {
		def jsonDir = "${pwd tmp: true}/json"
		def outputDir = "${pwd tmp: true}/output"
		def buildDir = "${pwd}/build"
		sh """
		mkdir -p ${jsonDir}
		rm -rf ${outputDir}
		mkdir -p ${outputDir}
		rsync -r -v --checksum --delete staging/ ${buildDir}
		make -C ${buildDir} -f guru/_scripts/GNUmakefile \
			JSON_DIR=${jsonDir} \
			OUTPUT_DIR=${outputDir} \
			VOLTA=bin/volta \
			DIODE=bin/diode
		rsync -r -v --checksum --delete ${outputDir}/ /opt/volt.guru
		"""
	}
}
