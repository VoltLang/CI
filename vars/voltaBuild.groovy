
import org.volt_lang.Builder;


def call() {
	def b = new Builder(steps)

	stage('Setup') {
		b.setup()
	}
	stage('Checkout') {
		b.checkoutVoltaSCM(scm)
	}
	stage('Distribute') {
		b.source()
	}
	stage('Bootstrap') {
		b.bootstrap()
	}
	stage('Build') {
		b.build()
	}
	stage('Test') {
		b.test()
	}
}
