
import org.volt_lang.Builder;

def call(Map config) {

	def b = new Builder(steps, scm)

	stage('Setup') {
		b.setupToolchain(config)
	}
}
