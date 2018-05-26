
import org.volt_lang.Builder;


def call() {
	def b = new Builder(steps, scm, env)

	stage('Setup') {
		b.setupVolta()
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
}
