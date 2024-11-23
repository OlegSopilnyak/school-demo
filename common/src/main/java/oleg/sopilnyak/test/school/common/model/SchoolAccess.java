package oleg.sopilnyak.test.school.common.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Model: Type for school's services access properties
 */
public interface SchoolAccess {
    /**
     * To get user-name for principal person's login
     *
     * @return value of user-name
     */
    String getLogin();

    /**
     * To check is it the correct password for login
     *
     * @param password password to check
     * @return true if password is correct
     */
    default boolean isPassword(String password) {
        return false;
    }

    /**
     * To make the signature for password
     *
     * @param password the password for signature
     * @return signature for login+password values
     * @throws NoSuchAlgorithmException if there is not MD5 algorithm
     */
    default String makeSignatureFor(String password) throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("MD5");
        final String forSign = getLogin() + " " + password;
        md.update(forSign.getBytes());
        return makeHexString(md.digest()).toUpperCase();
    }

    // private methods
    private static String makeHexString(final byte[] data) {
        final char[] hexCode = "0123456789ABCDEF".toCharArray();
        return IntStream.range(0, data.length).mapToObj(i -> {
            final byte dataByte = data[i];
            return String.valueOf(new char[]{
                    hexCode[(dataByte >>> 4) & 0xF],
                    hexCode[(dataByte & 0xF)]
            });
        }).collect(Collectors.joining());
    }
}
