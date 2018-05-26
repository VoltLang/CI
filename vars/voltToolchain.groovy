
import org.volt_lang.Builder;
import org.volt_lang.ToolchainConf;

def call(Map config) {

	def b = new Builder(steps, scm, env)
	def tc = new ToolchainConf(
		volta: config.volta,
		watt: config.watt,
		battery: config.battery,
		tag: env.TAG_NAME,
		)

	stage('Setup') {
		b.setupToolchain(tc)
	}
}
