package org.volt_lang


static def String makeToolchainDir(String arch, String plat, String root)
{
	return "${root}/toolchain-${arch}-${plat}"
}

static def String makeToolchainPkg(String arch, String plat, String tag)
{
	def file = "toolchain"

	if (tag != null) {
		def ver = tag.substring(1)
		file = "${file}-${ver}"
	}

	// Add arch and plat
	file = "${file}-${arch}-${plat}"

	// Add file ending
	if (plat == "msvc") {
		file = "${file}.zip"
	} else {
		file = "${file}.tar.gz"
	}

	return file
}
