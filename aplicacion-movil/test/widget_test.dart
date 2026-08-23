import 'package:aplicacion_movil/main.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('muestra el formulario de inicio de sesion', (tester) async {
    final session = SessionController(ApiClient('http://localhost:8080/api'));

    await tester.pumpWidget(MaterialApp(home: LoginPage(session: session)));

    expect(find.text('Iniciar sesion'), findsOneWidget);
    expect(find.byType(TextFormField), findsNWidgets(2));
  });
}
