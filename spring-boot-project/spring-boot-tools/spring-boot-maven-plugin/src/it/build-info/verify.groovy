import org.springframework.boot.maven.Verify

import static org.junit.Assert.assertTrue

def file = new File(basedir, "target/classes/META-INF/build-info.properties")
Properties properties = Verify.verifyBuildInfo(file,
		'org.springframework.boot.maven.it', 'build-info',
		'Generate build info', '0.0.1.BUILD-SNAPSHOT')
assertTrue properties.containsKey('build.time')