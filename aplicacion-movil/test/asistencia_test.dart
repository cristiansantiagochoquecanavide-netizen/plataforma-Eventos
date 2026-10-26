import 'dart:async';

import 'package:aplicacion_movil/main.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

class _SessionFalsa extends SessionController {
  _SessionFalsa() : super(ApiClient('http://localhost'));
  Future<Map<String, dynamic>> Function(String)? validar;
  Future<AttendanceModel> Function(int)? registrar;

  @override
  Future<Map<String, dynamic>> validarCodigoQr(String codigoQr) =>
      validar!(codigoQr);

  @override
  Future<AttendanceModel> registrarAsistencia(int idInscripcion) =>
      registrar!(idInscripcion);
}

class _FakeScannerPlatform extends MobileScannerPlatform {
  final StreamController<BarcodeCapture> _barcodes =
      StreamController<BarcodeCapture>.broadcast();

  @override
  Stream<BarcodeCapture?> get barcodesStream => _barcodes.stream;

  @override
  Stream<TorchState> get torchStateStream =>
      Stream.value(TorchState.unavailable);

  @override
  Stream<double> get zoomScaleStateStream => Stream.value(1);

  @override
  Future<MobileScannerViewAttributes> start(StartOptions startOptions) async =>
      const MobileScannerViewAttributes(
        cameraDirection: CameraFacing.back,
        currentTorchMode: TorchState.unavailable,
        size: Size(280, 280),
        numberOfCameras: 1,
      );

  @override
  Widget buildCameraView() => const ColoredBox(color: Colors.black);

  @override
  Future<void> stop() async {}

  @override
  Future<void> toggleTorch() async {}

  void scan(String value) =>
      _barcodes.add(BarcodeCapture(barcodes: [Barcode(rawValue: value)]));

  @override
  Future<void> dispose() => _barcodes.close();
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

  testWidgets(
    'valida el QR y confirma asistencia con la respuesta del backend',
    (tester) async {
      final scanner = _FakeScannerPlatform();
      MobileScannerPlatform.instance = scanner;
      final session = _SessionFalsa()
        ..validar = (codigoQr) async {
          expect(codigoQr, 'qr-token-valido');
          return {
            'idInscripcion': 22,
            'participante': 'Eva Rojas',
            'nombreEvento': 'Foro local',
          };
        }
        ..registrar = (_) async => respuesta;
      await tester.pumpWidget(
        MaterialApp(home: AttendancePage(session: session)),
      );
      await tester.pumpAndSettle();
      expect(find.text('Control de acceso'), findsOneWidget);
      scanner.scan('qr-token-valido');
      await tester.pumpAndSettle();
      expect(find.textContaining('Eva Rojas'), findsOneWidget);
      expect(find.text('Confirmar y registrar asistencia'), findsOneWidget);
      await tester.tap(find.text('Confirmar y registrar asistencia'));
      await tester.pumpAndSettle();
      expect(find.text('Asistencia registrada correctamente'), findsOneWidget);
      expect(find.textContaining('Eva Rojas'), findsOneWidget);
      expect(find.textContaining('25/10/2026'), findsOneWidget);
    },
  );

  testWidgets('muestra un error de validación y permite volver a escanear', (
    tester,
  ) async {
    final scanner = _FakeScannerPlatform();
    MobileScannerPlatform.instance = scanner;
    final session = _SessionFalsa();
    session.validar = (_) async => throw const ApiException('No válido', 404);
    session.registrar = (_) async => respuesta;
    await tester.pumpWidget(
      MaterialApp(home: AttendancePage(session: session)),
    );
    await tester.pumpAndSettle();
    scanner.scan('qr-token-invalido');
    await tester.pumpAndSettle();
    expect(
      find.text('El código QR no corresponde a una inscripción válida.'),
      findsOneWidget,
    );
    expect(find.text('Volver a escanear'), findsOneWidget);
  });

  testWidgets('informa check-in duplicado y conserva la inscripción validada', (
    tester,
  ) async {
    final scanner = _FakeScannerPlatform();
    MobileScannerPlatform.instance = scanner;
    final session = _SessionFalsa();
    session.validar = (_) async => {
      'idInscripcion': 22,
      'participante': 'Eva Rojas',
      'nombreEvento': 'Foro local',
    };
    session.registrar = (_) async => throw const ApiException('Repetido', 409);
    await tester.pumpWidget(
      MaterialApp(home: AttendancePage(session: session)),
    );
    await tester.pumpAndSettle();
    scanner.scan('qr-token-valido');
    await tester.pumpAndSettle();
    await tester.tap(find.text('Confirmar y registrar asistencia'));
    await tester.pumpAndSettle();
    expect(find.text('Esta asistencia ya fue registrada.'), findsOneWidget);
    expect(find.text('Inscripción validada'), findsOneWidget);
  });
}
