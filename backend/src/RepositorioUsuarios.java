
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;

public class RepositorioUsuarios {
    public RepositorioUsuarios() {
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized boolean existe(String username) {
        String key = norm(username);
        if (key.isEmpty()) return false;
        String sql = "SELECT 1 FROM usuarios WHERE username = ?";
        try (Connection conn = DataStore.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public synchronized boolean registrar(String username, String password) {
        if (username == null || password == null) return false;
        String key = norm(username);
        if (key.isEmpty()) return false;

        if (existe(key)) return false;

        String hashed = hashPassword(password);
        String sql = "INSERT INTO usuarios (username, password) VALUES (?, ?)";
        try (Connection conn = DataStore.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, hashed);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public synchronized boolean validar(String username, String password) {
        if (username == null || password == null) return false;
        String key = norm(username);
        if (key.isEmpty()) return false;

        String hashed = hashPassword(password);
        String sql = "SELECT password FROM usuarios WHERE username = ?";
        try (Connection conn = DataStore.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String dbPass = rs.getString("password");
                return dbPass != null && dbPass.equals(hashed);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static String norm(String username) {
        if (username == null) return "";
        return username.trim().toLowerCase();
    }
}
