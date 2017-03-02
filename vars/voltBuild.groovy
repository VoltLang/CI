
import org.volt_lang.Builder


def call(folder) {
	def b = new Builder(steps)

	stage('Setup') {
		b.setup()
	}
	stage('Checkout') {
		b.checkoutSCM(folder, scm)
	}
	stage('Distribute') {
		b.source()
	}
	stage('Toolchain') {
		b.toolchain()
	}
	stage('Build') {
		b.build()
	}
	stage('Archive') {
		b.archive()
	}
}
