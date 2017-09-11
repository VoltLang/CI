
import org.volt_lang.Builder


def call(folder) {
	def b = new Builder(steps)

	stage('Setup') {
		b.setup()
	}
	stage('Checkout') {
		b.checkoutSCM(folder, scm)
	}
	stage('Prepare') {
		b.prepare()
	}
	stage('Build') {
		b.build()
	}
	stage('Test') {
		b.test()
	}
	stage('Archive') {
		b.archive()
	}
}
