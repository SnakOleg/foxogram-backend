package su.foxogram.util;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

public class Totp {
	public static String generateKey() {
		SecretGenerator secretGenerator = new DefaultSecretGenerator();
		return secretGenerator.generate();
	}

	public static boolean validate(String userSecretKey, String userOTP) {
		TimeProvider timeProvider = new SystemTimeProvider();
		DefaultCodeGenerator codeGenerator = new DefaultCodeGenerator();
		CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

		return verifier.isValidCode(userSecretKey, userOTP);
	}
}
