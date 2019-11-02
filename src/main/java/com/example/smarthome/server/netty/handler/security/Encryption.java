package com.example.smarthome.server.netty.handler.security;

import javax.crypto.*;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

final class Encryption {

    private KeyPair serverKeyPair;
    private PublicKey clientPubKey;
    private SecretKeySpec serverAesKey;

    boolean isKeySet() {
        return serverAesKey != null;
    }

    byte[] encode(String s) {
        byte[] result = new byte[0];
        try {
            Cipher serverCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            serverCipher.init(Cipher.ENCRYPT_MODE, serverAesKey);
            byte[] data = serverCipher.doFinal(s.getBytes());
            // Передаёт клиенту параметры, с которыми выполнялась шифровка
            byte[] params = serverCipher.getParameters().getEncoded();

            byte[] tmp = new byte[params.length + data.length];
            System.arraycopy(params, 0, tmp, 0, params.length);
            System.arraycopy(data, 0, tmp, params.length, data.length);

            result = tmp;

        } catch (IOException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException |
                NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return result;
    }

    String decode(byte[] data, byte[] params) {
        String s = "";
        try {
            AlgorithmParameters aesParams = AlgorithmParameters.getInstance("AES");
            aesParams.init(params);
            Cipher clientCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            clientCipher.init(Cipher.DECRYPT_MODE, serverAesKey, aesParams);
            byte[] recovered = clientCipher.doFinal(data);
            s = new String(recovered);

        } catch (NoSuchAlgorithmException | IllegalBlockSizeException | IOException | NoSuchPaddingException
                | InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException e) {
            e.printStackTrace();
        }
        return s;
    }

    byte[] getPublicKey(byte[] clientPubKeyEncoded) {
        byte[] serverPubKeyEncoded = new byte[0];
        try {
            KeyFactory factory = KeyFactory.getInstance("DH");
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(clientPubKeyEncoded);
            clientPubKey = factory.generatePublic(x509KeySpec);
            // Параметры, которые использовал клиент при генерации ключей
            DHParameterSpec dhParamFromClientPubKey = ((DHPublicKey) clientPubKey).getParams();
            // Создаёт свою пару ключей. Отдаёт свой Encoded открытый ключ
            KeyPairGenerator serverGen = KeyPairGenerator.getInstance("DH");
            serverGen.initialize(dhParamFromClientPubKey);
            serverKeyPair = serverGen.generateKeyPair();
            serverPubKeyEncoded = serverKeyPair.getPublic().getEncoded();
            createSharedKey();

        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return serverPubKeyEncoded;
    }

    private void createSharedKey() {
        try {
            KeyAgreement serverKeyAgree = KeyAgreement.getInstance("DH");
            serverKeyAgree.init(serverKeyPair.getPrivate());
            serverKeyAgree.doPhase(clientPubKey, true);
            byte[] serverSharedSecret = serverKeyAgree.generateSecret();
            serverAesKey = new SecretKeySpec(serverSharedSecret, 0, 16, "AES");

        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
