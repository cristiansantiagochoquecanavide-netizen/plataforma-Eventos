import 'dart:async';
import 'package:aplicacion_movil/main.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

class _SessionFalsa extends SessionController {
  _SessionFalsa() : super(ApiClient('http://localhost'));
  Future<AttendanceModel> Function(int)? registrar;
  @override
  Future<AttendanceModel> registrarAsistencia(int idInscripcion) =>
      registrar!(idInscripcion);
}

void main() {
  final respuesta = AttendanceModel.fromJson({
    'idAsistencia': 1,
    'idInscripcion': 22,
    'participante': 'Eva Rojas',
    'nombreEvento': 'Foro local',
    'fechaHora': '2026-10-25T15:30:00Z',
    'nombreRegistradoPor': 'Staff demo',
  });

  test('interpreta la fecha y datos enviados por backend', () {
    expect(respuesta.idInscripcion, 22);
    expect(respuesta.fechaHora.toUtc(), DateTime.utc(2026, 10, 25, 15, 30));
  });

  test('lee ASISTENCIA_REGISTRAR de la membresía activa', () {
    final user = UserProfile.fromJson({
      'id': 9,
      'nombres': 'Rosa',
      'apellidos': 'Mora',
      'correo': 'rosa@example.com',
      'roles': ['STAFF'],
      'perfilOrganizacionActiva': {
        'idOrganizacion': 3,
        'roles': ['STAFF'],
        'permisos': ['ASISTENCIA_REGISTRAR'],
        'administrador': false,
      },
    });
    expect(user.permissions, contains('ASISTENCIA_REGISTRAR'));
  });

  testWidgets('carga formulario y muestra detalle con fecha recibida', (
    tester,
  ) async {
    final session = _SessionFalsa()..registrar = (_) async => respuesta;
    await tester.pumpWidget(
      MaterialApp(home: AttendancePage(session: session)),
    );
    expect(find.byType(FilledButton), findsOneWidget);
    await tester.enterText(find.byType(TextField), '22');
    await tester.tap(find.byType(FilledButton));
    await tester.pumpAndSettle();
    expect(find.text('Asistencia registrada correctamente.'), findsOneWidget);
    expect(find.textContaining('Eva Rojas'), findsOneWidget);
    expect(find.textContaining('25/10/2026'), findsOneWidget);
  });

  for (final testCase in <(int, String)>[
    (401, 'Tu sesión expiró. Inicia sesión nuevamente.'),
    (409, 'Esta asistencia ya fue registrada.'),
    (403, 'No tienes permiso para registrar asistencias.'),
    (404, 'No se puede registrar la asistencia.'),
    (400, 'No se puede registrar la asistencia.'),
    (500, 'No fue posible registrar la asistencia. Intenta nuevamente.'),
  ]) {
    testWidgets('muestra mensaje de HTTP ${testCase.$1}', (tester) async {
      final session = _SessionFalsa()
        ..registrar = (_) async =>
            throw ApiException('error interno', testCase.$1);
      await tester.pumpWidget(
        MaterialApp(home: AttendancePage(session: session)),
      );
      await tester.enterText(find.byType(TextField), '22');
      await tester.tap(find.byType(FilledButton));
      await tester.pumpAndSettle();
      expect(find.text(testCase.$2), findsOneWidget);
    });
  }

  testWidgets('deshabilita el envío mientras espera la respuesta', (
    tester,
  ) async {
    final completer = Completer<AttendanceModel>();
    final session = _SessionFalsa()..registrar = (_) => completer.future;
    await tester.pumpWidget(
      MaterialApp(home: AttendancePage(session: session)),
    );
    await tester.enterText(find.byType(TextField), '22');
    await tester.tap(find.byType(FilledButton));
    await tester.pump();
    final button = tester.widget<FilledButton>(find.byType(FilledButton));
    expect(button.onPressed, isNull);
    completer.complete(respuesta);
    await tester.pumpAndSettle();
  });
}
