import 'dart:convert';
import 'dart:async';

import 'package:aplicacion_movil/main.dart';
import 'package:aplicacion_movil/convocatorias/models/convocatoria_model.dart';
import 'package:aplicacion_movil/convocatorias/models/postulacion_staff_model.dart';
import 'package:aplicacion_movil/convocatorias/pages/convocatorias_page.dart';
import 'package:aplicacion_movil/convocatorias/pages/detalle_convocatoria_page.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

Map<String, dynamic> convocatoriaJson() => {
  'id': 2,
  'idEvento': 1,
  'nombreEvento': 'Congreso',
  'tipoPersonal': 'STAFF',
  'descripcion': 'Apoyo logistico',
  'cantidadRequerida': 5,
  'fechaLimite': DateTime.now()
      .add(const Duration(days: 2))
      .toIso8601String()
      .split('T')
      .first,
  'estado': 'ABIERTA',
  'fechaCreacion': '2026-01-01T10:00:00Z',
  'fechaActualizacion': '2026-01-01T10:00:00Z',
};

Map<String, dynamic> postulacionJson() => {
  'id': 3,
  'idUsuario': 4,
  'idConvocatoria': 2,
  'estado': 'PENDIENTE',
  'fechaPostulacion': '2026-01-01T10:00:00Z',
  'fechaActualizacion': '2026-01-01T10:00:00Z',
};

class FakeSession extends SessionController {
  FakeSession() : super(ApiClient('http://localhost/api'));
  PostulacionStaffModel? postulacion;
  ApiException? error;
  bool duplicado = false;
  ApiException? errorConsulta;
  int envios = 0;

  @override
  Future<List<InscripcionModel>> listarMisInscripciones() async => [];

  @override
  Future<List<ConvocatoriaModel>> listarConvocatorias() async => [
    ConvocatoriaModel.fromJson(convocatoriaJson()),
  ];
  @override
  Future<ConvocatoriaModel> obtenerConvocatoria(int id) async =>
      ConvocatoriaModel.fromJson(convocatoriaJson());
  @override
  Future<PostulacionStaffModel?> consultarPostulacion(int id) async {
    if (errorConsulta != null) throw errorConsulta!;
    return postulacion;
  }

  @override
  Future<PostulacionStaffModel> postularse(int id) async {
    envios++;
    if (error != null) throw error!;
    postulacion = PostulacionStaffModel.fromJson(postulacionJson());
    if (duplicado) {
      throw const ApiException(
        'El usuario ya se ha postulado a esta convocatoria',
        400,
      );
    }
    return postulacion!;
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const tokens = SessionTokens(
    accessToken: 'jwt-prueba',
    refreshToken: 'refresco',
  );
  final api = ApiClient('http://localhost/api');

  test('fallo de red al renovar conserva la sesion', () async {
    FlutterSecureStorage.setMockInitialValues({
      'sesion': jsonEncode(tokens.toJson()),
    });
    final session = SessionController(api);
    await http.runWithClient(
      () async {
        await session.restore();
        await expectLater(session.postularse(2), throwsA(isA<ApiException>()));
        expect(session.user, isNotNull);
        expect(
          await const FlutterSecureStorage().read(key: 'sesion'),
          isNotNull,
        );
      },
      () => MockClient((request) async {
        if (request.url.path.endsWith('/usuario-actual')) {
          return http.Response(
            '{"id":4,"nombres":"Ana","apellidos":"Perez","correo":"ana@example.com","roles":["STAFF"]}',
            200,
          );
        }
        if (request.url.path.endsWith('/refrescar')) {
          throw http.ClientException('sin red');
        }
        return http.Response('{"message":"vencido"}', 401);
      }),
    );
  });

  test('peticiones concurrentes comparten una renovacion JWT', () async {
    FlutterSecureStorage.setMockInitialValues({
      'sesion': jsonEncode(tokens.toJson()),
    });
    final session = SessionController(api);
    var renovaciones = 0;
    var originales = 0;
    final ambasPeticiones = Completer<void>();
    await http.runWithClient(
      () async {
        await session.restore();
        await Future.wait([
          session.listarConvocatorias(),
          session.listarMisPostulaciones(),
        ]);
        expect(renovaciones, 1);
        expect(session.user, isNotNull);
      },
      () => MockClient((request) async {
        if (request.url.path.endsWith('/usuario-actual')) {
          return http.Response(
            '{"id":4,"nombres":"Ana","apellidos":"Perez","correo":"ana@example.com","roles":["STAFF"]}',
            200,
          );
        }
        if (request.url.path.endsWith('/refrescar')) {
          renovaciones++;
          await ambasPeticiones.future;
          return http.Response(
            '{"accessToken":"renovado","refreshToken":"nuevo"}',
            200,
          );
        }
        if (request.headers['Authorization'] == 'Bearer jwt-prueba') {
          originales++;
          if (originales == 2) ambasPeticiones.complete();
          return http.Response('{"message":"vencido"}', 401);
        }
        return http.Response('[]', 200);
      }),
    );
  });

  testWidgets(
    'el detalle sigue visible si consultar postulacion devuelve 403',
    (tester) async {
      final session = FakeSession()
        ..errorConsulta = const ApiException(
          'Sin permiso para postularse',
          403,
        );
      await tester.pumpWidget(
        MaterialApp(
          home: DetalleConvocatoriaPage(session: session, idConvocatoria: 2),
        ),
      );
      await tester.pumpAndSettle();
      expect(find.text('Congreso'), findsOneWidget);
      expect(find.text('Sin permiso para postularse'), findsOneWidget);
      expect(
        tester.widget<FilledButton>(find.byType(FilledButton)).onPressed,
        isNull,
      );
    },
  );

  testWidgets('cambiar pestaña conserva los cambios pendientes del perfil', (
    tester,
  ) async {
    await tester.binding.setSurfaceSize(const Size(800, 1200));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final session = FakeSession()
      ..user = const UserProfile(
        id: 4,
        names: 'Ana',
        lastNames: 'Perez',
        email: 'ana@example.com',
        roles: ['STAFF'],
      );
    await tester.pumpWidget(
      MaterialApp(home: HomeShell(session: session)),
    );
    await tester.pumpAndSettle();
    expect(find.text('Eventos disponibles'), findsOneWidget);
    await tester.tap(find.text('Inscripciones'));
    await tester.pumpAndSettle();
    expect(find.byType(MyInscriptionsPage), findsOneWidget);
    await tester.tap(find.text('Mi Perfil'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField).first, 'Nombre sin guardar');
    await tester.tap(find.text('Convocatorias'));
    await tester.pumpAndSettle();
    expect(find.text('Forma parte del equipo'), findsOneWidget);
    await tester.tap(find.text('Mi Perfil'));
    await tester.pumpAndSettle();
    expect(find.text('Nombre sin guardar'), findsOneWidget);
  });

  for (final renovacionValida in [true, false]) {
    test(
      '401: ${renovacionValida ? 'renueva JWT y conserva sesion ante 400' : 'cierra sesion si no puede renovar'}',
      () async {
        FlutterSecureStorage.setMockInitialValues({
          'sesion': jsonEncode(tokens.toJson()),
        });
        final session = SessionController(api);
        var intentos = 0;
        await http.runWithClient(
          () async {
            await session.restore();
            expect(session.user, isNotNull);
            await expectLater(
              session.postularse(2),
              throwsA(
                isA<ApiException>().having(
                  (error) => error.status,
                  'status',
                  renovacionValida ? 400 : 401,
                ),
              ),
            );
            expect(session.user, renovacionValida ? isNotNull : isNull);
            expect(intentos, renovacionValida ? 2 : 1);
          },
          () => MockClient((request) async {
            if (request.url.path.endsWith('/usuario-actual')) {
              return http.Response(
                jsonEncode({
                  'id': 4,
                  'nombres': 'Ana',
                  'apellidos': 'Perez',
                  'correo': 'ana@example.com',
                  'roles': ['STAFF'],
                }),
                200,
              );
            }
            if (request.url.path.endsWith('/refrescar')) {
              return renovacionValida
                  ? http.Response(
                      '{"accessToken":"renovado","refreshToken":"nuevo"}',
                      200,
                    )
                  : http.Response('{"message":"Sesion vencida"}', 401);
            }
            intentos++;
            if (intentos == 1) {
              return http.Response('{"message":"Token vencido"}', 401);
            }
            expect(request.headers['Authorization'], 'Bearer renovado');
            return http.Response('{"message":"Ya postulado"}', 400);
          }),
        );
      },
    );
  }

  test('las seis rutas envian JWT y POST no envia usuario', () async {
    final rutas = <String>[];
    await http.runWithClient(
      () async {
        expect(await api.listarConvocatorias(tokens), hasLength(1));
        expect(
          (await api.obtenerConvocatoria(tokens, 2)).nombreEvento,
          'Congreso',
        );
        expect((await api.postularse(tokens, 2)).estado, 'PENDIENTE');
        expect(await api.consultarPostulacion(tokens, 2), isNull);
        expect(await api.listarMisPostulaciones(tokens), hasLength(1));
        expect((await api.retirarPostulacion(tokens, 3)).id, 3);
      },
      () => MockClient((request) async {
        expect(request.headers['Authorization'], 'Bearer jwt-prueba');
        rutas.add('${request.method} ${request.url.path}');
        if (request.method == 'POST') expect(jsonDecode(request.body), isEmpty);
        if (request.url.path.endsWith('/postulacion')) {
          return http.Response('', 204);
        }
        if (request.url.path.endsWith('/convocatorias')) {
          expect(request.url.queryParameters['estado'], 'ABIERTA');
          return http.Response(jsonEncode([convocatoriaJson()]), 200);
        }
        if (request.url.path.endsWith('/convocatorias/2')) {
          return http.Response(jsonEncode(convocatoriaJson()), 200);
        }
        if (request.url.path.endsWith('/mias')) {
          return http.Response(jsonEncode([postulacionJson()]), 200);
        }
        return http.Response(jsonEncode(postulacionJson()), 200);
      }),
    );
    expect(rutas, [
      'GET /api/convocatorias',
      'GET /api/convocatorias/2',
      'POST /api/convocatorias/2/postulaciones',
      'GET /api/convocatorias/2/postulacion',
      'GET /api/postulaciones/mias',
      'PATCH /api/postulaciones/3/retirar',
    ]);
  });

  for (final status in [400, 401]) {
    test('preserva error HTTP $status', () async {
      await http.runWithClient(
        () async {
          await expectLater(
            api.postularse(tokens, 2),
            throwsA(
              isA<ApiException>()
                  .having((error) => error.status, 'status', status)
                  .having(
                    (error) => error.message,
                    'message',
                    'Mensaje del servidor',
                  ),
            ),
          );
        },
        () => MockClient(
          (_) async =>
              http.Response('{"message":"Mensaje del servidor"}', status),
        ),
      );
    });
  }

  testWidgets('lista evento, tipo, descripcion y fecha', (tester) async {
    await tester.pumpWidget(
      MaterialApp(home: ConvocatoriasPage(session: FakeSession())),
    );
    await tester.pumpAndSettle();
    expect(find.text('Congreso'), findsOneWidget);
    expect(find.text('STAFF'), findsOneWidget);
    expect(find.text('Apoyo logistico'), findsOneWidget);
    expect(find.textContaining('Fecha limite:'), findsOneWidget);
  });

  for (final duplicado in [false, true]) {
    testWidgets(
      'actualiza estado despues de ${duplicado ? 'duplicado' : 'postular'}',
      (tester) async {
        await tester.binding.setSurfaceSize(const Size(800, 1200));
        addTearDown(() => tester.binding.setSurfaceSize(null));
        final session = FakeSession()..duplicado = duplicado;
        await tester.pumpWidget(
          MaterialApp(
            home: DetalleConvocatoriaPage(session: session, idConvocatoria: 2),
          ),
        );
        await tester.pumpAndSettle();
        await tester.tap(find.text('Postularme'));
        await tester.pumpAndSettle();
        expect(find.text('Tu postulacion: PENDIENTE'), findsOneWidget);
        expect(find.text('Postularme'), findsNothing);
        expect(session.envios, 1);
        if (duplicado) {
          expect(find.textContaining('ya se ha postulado'), findsOneWidget);
        } else {
          expect(
            find.text('Tu postulacion se registro correctamente.'),
            findsOneWidget,
          );
        }
      },
    );
  }

  testWidgets('postulacion existente impide nuevo envio', (tester) async {
    final session = FakeSession()
      ..postulacion = PostulacionStaffModel.fromJson(postulacionJson());
    await tester.pumpWidget(
      MaterialApp(
        home: DetalleConvocatoriaPage(session: session, idConvocatoria: 2),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Tu postulacion: PENDIENTE'), findsOneWidget);
    expect(find.text('Postularme'), findsNothing);
    expect(session.envios, 0);
  });
}
