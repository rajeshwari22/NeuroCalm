/// Demo authentication. Replace the body of [login] with a real backend call
/// (see ApiConfig.login in backend_service.dart).
class AuthService {
  AuthService._();
  static final instance = AuthService._();

  String? email;
  bool get isLoggedIn => email != null;

  Future<bool> login(String email, String password) async {
    await Future.delayed(const Duration(milliseconds: 600)); // simulated latency
    // TODO: POST credentials to your backend and store the returned token.
    this.email = email;
    return true;
  }

  void logout() => email = null;
}
