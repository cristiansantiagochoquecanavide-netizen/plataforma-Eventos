import 'package:aplicacion_movil/main.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('parsea la respuesta de organizaciones del backend', () {
    final organization = OrganizationModel.fromJson({
      'id': 7,
      'nombre': 'Comunidad Innovacion',
      'slug': 'innovacion',
      'estado': 'ACTIVA',
    });

    expect(organization.id, 7);
    expect(organization.name, 'Comunidad Innovacion');
    expect(organization.slug, 'innovacion');
  });

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

  testWidgets('renderiza HomeShell con sus tres destinos de navegacion', (
    tester,
  ) async {
    final session = SessionController(ApiClient('http://localhost:8080/api'));
    session.user = const UserProfile(
      id: 1,
      names: 'Carlos',
      lastNames: 'Mendez',
      email: 'carlos@eventos.local',
      roles: ['PARTICIPANTE'],
    );

    await tester.pumpWidget(MaterialApp(home: HomeShell(session: session)));

    expect(find.text('Eventos'), findsWidgets);
    expect(find.text('Inscripciones'), findsOneWidget);
    expect(find.text('Mi Perfil'), findsOneWidget);
  });

  testWidgets('informa cuando la sesion no tiene organizaciones', (
    tester,
  ) async {
    final session = SessionController(ApiClient('http://localhost:8080/api'))
      ..user = const UserProfile(
        id: 1,
        names: 'Carlos',
        lastNames: 'Mendez',
        email: 'carlos@eventos.local',
        roles: ['PARTICIPANTE'],
      )
      ..organizationsLoaded = true;

    await tester.pumpWidget(MaterialApp(home: HomeShell(session: session)));

    expect(find.text('Organizacion requerida'), findsOneWidget);
    expect(
      find.textContaining('no pertenece a ninguna organizacion'),
      findsOneWidget,
    );
  });

  testWidgets('muestra la pantalla de Mis Inscripciones con su encabezado', (
    tester,
  ) async {
    final session = SessionController(ApiClient('http://localhost:8080/api'));
    session.user = const UserProfile(
      id: 1,
      names: 'Carlos',
      lastNames: 'Mendez',
      email: 'carlos@eventos.local',
      roles: ['PARTICIPANTE'],
    );

    await tester.pumpWidget(
      MaterialApp(home: MyInscriptionsPage(session: session)),
    );

    expect(find.text('Mis inscripciones'), findsOneWidget);
    expect(find.byIcon(Icons.refresh_rounded), findsOneWidget);
  });

  testWidgets('renderiza el modal de detalle de evento con fecha y aforo', (
    tester,
  ) async {
    final session = SessionController(ApiClient('http://localhost:8080/api'));
    final event = EventModel(
      id: 5,
      name: 'Hackathon Innovacion',
      description: 'Competencia tecnologica de desarrollo agil',
      category: 'TECNOLOGIA',
      start: DateTime.now().add(const Duration(days: 3)),
      end: DateTime.now().add(const Duration(days: 4)),
      place: 'Auditorio Principal',
      status: 'PUBLICADO',
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: EventDetailModal(event: event, session: session),
        ),
      ),
    );

    expect(find.text('Hackathon Innovacion'), findsOneWidget);
    expect(find.text('TECNOLOGIA'), findsOneWidget);
    expect(find.text('Fecha de inicio'), findsOneWidget);
    expect(find.text('Auditorio Principal'), findsOneWidget);
  });
}
