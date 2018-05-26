
import org.volt_lang.Builder;
import org.volt_lang.ToolchainConf;

def call(Map config) {

	def b = new Builder(steps, scm, env)
	def tc = new ToolchainConf(
		tag: env.TAG_NAME,
		voltaTag: config.volta,
		wattTag: config.watt,
		batteryTag: config.battery,
		)

	stage('Setup') {
		b.setupToolchain(tc)
	}
}
