import 'package:flutter_test/flutter_test.dart';
import 'package:neurocalm_app/main.dart';

void main() {
  testWidgets('Login screen shows tagline', (tester) async {
    await tester.pumpWidget(const NeuroCalmApp());
    expect(find.text('Calm Mind. Healthy You.'), findsOneWidget);
  });
}
