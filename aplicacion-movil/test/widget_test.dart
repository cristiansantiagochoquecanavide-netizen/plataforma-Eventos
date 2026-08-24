import 'package:aplicacion_movil/main.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('muestra el formulario de inicio de sesion', (tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 1200));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final session = SessionController(ApiClient('http://localhost:8080/api'));

    await tester.pumpWidget(MaterialApp(home: LoginPage(session: session)));

    expect(find.text('Bienvenido'), findsOneWidget);
    expect(find.text('Entrar a mi cuenta'), findsOneWidget);
    expect(find.byType(TextFormField), findsNWidgets(2));
  });

  testWidgets('permite navegar a crear una cuenta', (tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 1200));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final session = SessionController(ApiClient('http://localhost:8080/api'));

    await tester.pumpWidget(MaterialApp(home: LoginPage(session: session)));
    await tester.tap(find.text('Crear cuenta'));
    await tester.pumpAndSettle();

    expect(find.text('Registro de participante'), findsOneWidget);
  });

  testWidgets('muestra la cuenta autenticada', (tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 1200));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final session = SessionController(ApiClient('http://localhost:8080/api'));
    session.user = const UserProfile(
      id: 1,
      names: 'Ana',
      lastNames: 'Perez',
      email: 'ana@eventos.local',
      roles: ['PARTICIPANTE'],
    );

    await tester.pumpWidget(MaterialApp(home: ProfilePage(session: session)));

    expect(find.text('Hola, Ana'), findsOneWidget);
    expect(find.text('Datos personales'), findsOneWidget);
    expect(find.text('Actualizar clave'), findsOneWidget);
  });

  testWidgets('precarga el token recibido en el enlace de recuperacion', (
    tester,
  ) async {
    final session = SessionController(ApiClient('http://localhost:8080/api'));

    await tester.pumpWidget(
      MaterialApp(
        home: PasswordRecoveryPage(
          session: session,
          tokenInicial: 'token-prueba',
        ),
      ),
    );

    expect(find.text('Restablecer clave'), findsNWidgets(2));
    expect(find.text('Confirmar nueva clave'), findsOneWidget);
    expect(
      tester.widget<TextField>(find.byType(TextField).first).controller!.text,
      'token-prueba',
    );
  });
}
