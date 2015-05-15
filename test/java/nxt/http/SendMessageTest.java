package nxt.http;

import nxt.Account;
import nxt.BlockchainTest;
import nxt.Constants;
import nxt.crypto.Crypto;
import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class SendMessageTest extends BlockchainTest {

    @Test
    public void sendMessage() {
        JSONObject response = new APICall.Builder("sendMessage").
                param("secretPhrase", testers.get(1).getSecretPhrase()).
                param("recipient", testers.get(2).getStrId()).
                param("message", "hello world").
                param("feeNQT", Constants.ONE_NXT).
                build().invoke();
        Logger.logDebugMessage("sendMessage: " + response);
        String transaction = (String) response.get("transaction");
        JSONObject attachment = (JSONObject) ((JSONObject)response.get("transactionJSON")).get("attachment");
        Assert.assertEquals("hello world", attachment.get("message"));
        generateBlock();
        response = new APICall.Builder("readMessage").
                param("secretPhrase", testers.get(2).getSecretPhrase()).
                param("transaction", transaction).
                build().invoke();
        Logger.logDebugMessage("readMessage: " + response);
        Assert.assertEquals("hello world", response.get("message"));
    }

    @Test
    public void sendEncryptedMessage() {
        JSONObject response = new APICall.Builder("sendMessage").
                param("secretPhrase", testers.get(1).getSecretPhrase()).
                param("recipient", testers.get(2).getStrId()).
                param("messageToEncrypt", "hello world").
                param("feeNQT", Constants.ONE_NXT).
                build().invoke();
        Logger.logDebugMessage("sendMessage: " + response);
        String transaction = (String) response.get("transaction");
        JSONObject attachment = (JSONObject) ((JSONObject)response.get("transactionJSON")).get("attachment");
        JSONObject encryptedMessage = (JSONObject) attachment.get("encryptedMessage");
        Assert.assertNotEquals(64, ((String) encryptedMessage.get("data")).length());
        Assert.assertNotEquals(32, ((String) encryptedMessage.get("nonce")).length());
        generateBlock();
        response = new APICall.Builder("readMessage").
                param("secretPhrase", testers.get(2).getSecretPhrase()).
                param("transaction", transaction).
                build().invoke();
        Logger.logDebugMessage("readMessage: " + response);
        Assert.assertEquals("hello world", response.get("decryptedMessage"));
    }

    @Test
    public void sendClientEncryptedMessage() {
        EncryptedData encryptedData = testers.get(2).getAccount().encryptTo(Convert.toBytes("hello world"), testers.get(1).getSecretPhrase(), true);
        JSONObject response = new APICall.Builder("sendMessage").
                param("secretPhrase", testers.get(1).getSecretPhrase()).
                param("recipient", testers.get(2).getStrId()).
                param("encryptedMessageData", Convert.toHexString(encryptedData.getData())).
                param("encryptedMessageNonce", Convert.toHexString(encryptedData.getNonce())).
                param("feeNQT", Constants.ONE_NXT).
                build().invoke();
        Logger.logDebugMessage("sendMessage: " + response);
        String transaction = (String) response.get("transaction");
        JSONObject attachment = (JSONObject) ((JSONObject)response.get("transactionJSON")).get("attachment");
        JSONObject encryptedMessage = (JSONObject) attachment.get("encryptedMessage");
        Assert.assertNotEquals(64, ((String) encryptedMessage.get("data")).length());
        Assert.assertNotEquals(32, ((String) encryptedMessage.get("nonce")).length());
        generateBlock();
        response = new APICall.Builder("readMessage").
                param("secretPhrase", testers.get(2).getSecretPhrase()).
                param("transaction", transaction).
                build().invoke();
        Logger.logDebugMessage("readMessage: " + response);
        Assert.assertEquals("hello world", response.get("decryptedMessage"));
    }

    @Test
    public void sendEncryptedMessageToSelf() {
        JSONObject response = new APICall.Builder("sendMessage").
                param("secretPhrase", testers.get(1).getSecretPhrase()).
                param("recipient", testers.get(2).getStrId()).
                param("messageToEncryptToSelf", "hello world").
                param("feeNQT", Constants.ONE_NXT).
                build().invoke();
        Logger.logDebugMessage("sendMessage: " + response);
        String transaction = (String) response.get("transaction");
        JSONObject attachment = (JSONObject) ((JSONObject)response.get("transactionJSON")).get("attachment");
        JSONObject encryptedMessage = (JSONObject) attachment.get("encryptToSelfMessage");
        Assert.assertNotEquals(64, ((String) encryptedMessage.get("data")).length());
        Assert.assertNotEquals(32, ((String) encryptedMessage.get("nonce")).length());
        generateBlock();
        response = new APICall.Builder("readMessage").
                param("secretPhrase", testers.get(1).getSecretPhrase()).
                param("transaction", transaction).
                build().invoke();
        Logger.logDebugMessage("readMessage: " + response);
        Assert.assertEquals("hello world", response.get("decryptedMessageToSelf"));
    }

    @Test
    public void sendClientEncryptedMessageToSelf() {
        EncryptedData encryptedData = testers.get(1).getAccount().encryptTo(Convert.toBytes("hello world"), testers.get(1).getSecretPhrase(), true);
        JSONObject response = new APICall.Builder("sendMessage").
                param("secretPhrase", testers.get(1).getSecretPhrase()).
                param("recipient", testers.get(2).getStrId()).
                param("encryptToSelfMessageData", Convert.toHexString(encryptedData.getData())).
                param("encryptToSelfMessageNonce", Convert.toHexString(encryptedData.getNonce())).
                param("feeNQT", Constants.ONE_NXT).
                build().invoke();
        Logger.logDebugMessage("sendMessage: " + response);
        String transaction = (String) response.get("transaction");
        JSONObject attachment = (JSONObject) ((JSONObject)response.get("transactionJSON")).get("attachment");
        JSONObject encryptedMessage = (JSONObject) attachment.get("encryptToSelfMessage");
        Assert.assertEquals(64 + 32 /* data + hash */, ((String) encryptedMessage.get("data")).length());
        Assert.assertEquals(64, ((String) encryptedMessage.get("nonce")).length());
        generateBlock();
        response = new APICall.Builder("readMessage").
                param("secretPhrase", testers.get(1).getSecretPhrase()).
                param("transaction", transaction).
                build().invoke();
        Logger.logDebugMessage("readMessage: " + response);
        Assert.assertEquals("hello world", response.get("decryptedMessageToSelf"));
    }

    @Test
    public void publicKeyAnnouncement() {
        byte[] publicKey = Crypto.getPublicKey("NonExistentAccount.jkgdkjgdjkfgfkjgfjkdfgkjjdk");
        String publicKeyStr = Convert.toHexString(publicKey);
        long id = Account.getId(publicKey);
        String rsAccount = Convert.rsAccount(id);

        JSONObject response = new APICall.Builder("getAccount").
                param("account", rsAccount).
                build().invoke();
        Logger.logDebugMessage("getAccount: " + response);
        Assert.assertEquals((long) 5, response.get("errorCode"));

        response = new APICall.Builder("sendMessage").
                param("secretPhrase", testers.get(1).getSecretPhrase()).
                param("recipient", rsAccount).
                param("recipientPublicKey", publicKeyStr).
                param("feeNQT", Constants.ONE_NXT).
                build().invoke();
        Logger.logDebugMessage("sendMessage: " + response);
        generateBlock();

        response = new APICall.Builder("getAccount").
                param("account", rsAccount).
                build().invoke();
        Logger.logDebugMessage("getAccount: " + response);
        Assert.assertEquals(publicKeyStr, response.get("publicKey"));
    }
}
