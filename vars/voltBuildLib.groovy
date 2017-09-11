
import org.volt_lang.Builder


def call(folder) {
	def b = new Builder(steps, scm)

	stage('Setup') {
		b.setupLib(folder)
	}
	stage('Checkout') {
		b.checkout()
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
