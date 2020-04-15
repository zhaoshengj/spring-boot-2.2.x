import java.io.*;
import org.springframework.boot.maven.*;

Verify.verifyJar(
	new File(basedir, "target/jar-executable-0.0.1.BUILD-SNAPSHOT.jar"),
	"some.random.Main", "Spring Boot Startup Script", "MyFullyExecutableJarName",
	"MyFullyExecutableJarDesc")
