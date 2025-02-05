package su.foxogram.util;

import org.mindrot.jbcrypt.BCrypt;

public class Encryptor {
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public static boolean verifyPassword(String password, String hashedPassword) {
        try {
            return !BCrypt.checkpw(password, hashedPassword);
        } catch(Exception error) {
            error.printStackTrace();
            return true;
        }
    }
}
