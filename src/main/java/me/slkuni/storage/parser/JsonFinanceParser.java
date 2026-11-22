package me.slkuni.storage.parser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.slkuni.model.Account;
import me.slkuni.model.WalletSnapshot;
import me.slkuni.storage.SavedWallet;
import me.slkuni.validation.LoginValidator;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonFinanceParser implements FinanceParser {
    private static final int FORMAT_VERSION = 1;

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

    @Override
    public Map<String, SavedWallet> readState(byte[] data) throws IOException {
        JsonNode root = readRoot(data);
        JsonNode users = root.path("users");
        if (!users.isObject()) {
            throw new IOException("Не найден список аккаунтов.");
        }

        Map<String, SavedWallet> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> entry : users.properties()) {
            String login = entry.getKey();
            JsonNode user = entry.getValue();
            Account account = readAccount(login, user.path("account"));
            WalletSnapshot wallet = readSnapshot(user.path("wallet"));
            result.put(login, new SavedWallet(account, wallet));
        }
        return result;
    }

    @Override
    public byte[] writeState(Map<String, SavedWallet> wallets) throws IOException {
        ObjectNode root = mapper.createObjectNode();
        root.put("version", FORMAT_VERSION);
        root.set("users", mapper.valueToTree(wallets));
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);
    }

    @Override
    public WalletSnapshot readWallet(byte[] data, String login) throws IOException {
        JsonNode root = readRoot(data);
        if (!login.equals(root.path("login").textValue())) {
            throw new IllegalArgumentException("Снимок принадлежит другому логину.");
        }
        return readSnapshot(root.path("wallet"));
    }

    @Override
    public byte[] writeWallet(String login, WalletSnapshot snapshot) throws IOException {
        ObjectNode root = mapper.createObjectNode();
        root.put("version", FORMAT_VERSION);
        root.put("login", login);
        root.set("wallet", mapper.valueToTree(snapshot));
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);
    }

    private JsonNode readRoot(byte[] data) throws IOException {
        JsonNode root = mapper.readTree(data);
        if (root == null || !root.isObject() || !root.path("version").isIntegralNumber()
                || root.path("version").asInt() != FORMAT_VERSION) {
            throw new IOException("Некорректный формат файла данных.");
        }
        return root;
    }

    private WalletSnapshot readSnapshot(JsonNode node) throws IOException {
        if (!node.isObject()) {
            throw new IOException("В файле отсутствует кошелёк.");
        }
        return mapper.treeToValue(node, WalletSnapshot.class);
    }

    private Account readAccount(String login, JsonNode node) throws IOException {
        String hash = node.path("passwordHash").textValue();
        if (hash == null || !hash.matches("[a-f0-9]{32}")) {
            throw new IOException("Для аккаунта \"" + login + "\" ожидается хеш пароля MD5.");
        }
        String savedLogin = node.path("login").textValue();
        if (!LoginValidator.isValid(login) || !login.equals(savedLogin)) {
            throw new IOException("Некорректные данные аккаунта \"" + login + "\".");
        }
        return new Account(login, hash);
    }
}
