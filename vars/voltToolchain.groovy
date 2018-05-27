
import org.volt_lang.Builder
import org.volt_lang.ToolchainConf
import static org.volt_lang.Helpers.*


def call(Map config) {

	def builder = new Builder(steps, scm, env)
	def toolchainConf = new ToolchainConf(
		tag: env.TAG_NAME,
		voltaTag: config.volta,
		wattTag: config.watt,
		batteryTag: config.battery,
		)

	stage('Setup') {
		builder.setupToolchain(toolchainConf)
	}

	stage('Checkout') {
		builder.checkout()
	}

	stage('Prepare') {
		builder.prepare()
	}

	stage('Build') {
		builder.build()
	}

	stage('Test') {
		builder.test()
	}

	stage('Archive') {
		builder.dispatch(this.&doToolchain, true, toolchainConf)
	}
}

def doToolchain(root, arch, plat, arg)
{
	def toolchainConf = arg
	def name = "${arch}-${plat}"

	def pkgFile = makeToolchainPkg(arch, plat, toolchainConf.tag)
	def stageDir = makeToolchainDir(arch, plat, root)
	def exe = plat == 'msvc' ? '.exe' : ''

	// Use zip for msvc or tar for nix.
	def cmd = plat == 'msvc' ? 'zip -q -r' : 'tar -czf'

	dir(stageDir) {
		deleteDir()
		sh """
		mkdir -p bin
		mkdir -p lib
		cp ../${name}/volta${exe} bin
		cp ../${name}/battery${exe} bin
		cp -r ../src/volta/rt lib
		cp -r ../src/watt lib
		rm -rf `find lib/rt -type d -name test`
		rm -rf `find lib/watt -type d -name test`
		${cmd} ${pkgFile} *
		"""
		archiveArtifacts artifacts: pkgFile, fingerprint: true, onlyIfSuccessful: true
	}
}
