import 'package:aplicacion_movil/core/ui_privileges/ui_privilege_service.dart';
import 'package:aplicacion_movil/core/ui_privileges/ui_privilege_widgets.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('visible wrapper hides when VISIBLE is not granted', (tester) async {
    final service = UiPrivilegeService();

    await tester.pumpWidget(
      MaterialApp(
        home: UiVisible(
          service: service,
          privilege: 'inscriptions.list',
          child: const Text('Allowed'),
        ),
      ),
    );

    expect(find.text('Allowed'), findsNothing);
  });

  testWidgets('enabled builder combines privilege and business disabled', (
    tester,
  ) async {
    final service = UiPrivilegeService();
    await service.load(
      () async => {
        'componentes': {
          'event.registration.create': {
            'estadosPermitidos': ['VISIBLE', 'HABILITADO'],
          },
        },
      },
    );

    await tester.pumpWidget(
      MaterialApp(
        home: UiEnabledBuilder(
          service: service,
          privilege: 'event.registration.create',
          businessDisabled: true,
          builder: (context, enabled) => ElevatedButton(
            onPressed: enabled ? () {} : null,
            child: const Text('Register'),
          ),
        ),
      ),
    );

    expect(
      tester.widget<ElevatedButton>(find.byType(ElevatedButton)).onPressed,
      isNull,
    );
  });

  testWidgets('editable builder exposes read only when EDITABLE is missing', (
    tester,
  ) async {
    final service = UiPrivilegeService();
    await service.load(
      () async => {
        'componentes': {
          'profile.edit': {
            'estadosPermitidos': ['VISIBLE'],
          },
        },
      },
    );

    await tester.pumpWidget(
      MaterialApp(
        home: UiEditableBuilder(
          service: service,
          privilege: 'profile.edit',
          builder: (context, readOnly) => TextField(readOnly: readOnly),
        ),
      ),
    );

    expect(tester.widget<TextField>(find.byType(TextField)).readOnly, isTrue);
  });
}
