import 'dart:async';

import 'package:aplicacion_movil/core/ui_privileges/ui_privilege_service.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('parses backend privileges and denies missing keys by default', () {
    final snapshot = UiPrivilegeSnapshot.fromJson({
      'componentes': {
        'events.filters': {
          'estadosPermitidos': ['VISIBLE', 'EDITABLE'],
        },
        'event.registration.create': {
          'estadosPermitidos': ['VISIBLE', 'HABILITADO'],
        },
      },
    });

    expect(snapshot.visible('events.filters'), isTrue);
    expect(snapshot.editable('events.filters'), isTrue);
    expect(snapshot.enabled('events.filters'), isFalse);
    expect(snapshot.visible('missing.key'), isFalse);
  });

  test('requires visible before habilitado or editable are effective', () {
    final snapshot = UiPrivilegeSnapshot.fromJson({
      'componentes': {
        'inscriptions.cancel': {
          'estadosPermitidos': ['HABILITADO'],
        },
        'profile.edit': {
          'estadosPermitidos': ['EDITABLE'],
        },
      },
    });

    expect(snapshot.enabled('inscriptions.cancel'), isFalse);
    expect(snapshot.editable('profile.edit'), isFalse);
  });

  test('clear prevents stale requests from replacing the empty snapshot', () async {
    final service = UiPrivilegeService();
    final completer = Completer<Map<String, dynamic>>();

    final load = service.load(() => completer.future);
    service.clear();
    completer.complete({
      'componentes': {
        'inscriptions.list': {
          'estadosPermitidos': ['VISIBLE'],
        },
      },
    });
    await load;

    expect(service.visible('inscriptions.list'), isFalse);
    expect(service.isLoading, isFalse);
  });
}
