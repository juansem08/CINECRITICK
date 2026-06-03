public class SOLID_CineCritik {
    public interface Autenticable {
        boolean validar(String usr, String pwd);
    }
    static class GestorAcceso implements Autenticable {
        private String username;
        private String password;
        public GestorAcceso(String u, String p) {
            username = u; password = p;
        }
        @Override
        public boolean validar(String usr, String pwd) {
            if (usr==null||pwd==null) return false;
            return usr.equals(username)&&pwd.equals(password);
        }
    }
}
