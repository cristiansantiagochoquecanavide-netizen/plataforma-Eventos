import 'package:flutter/widgets.dart';

import 'ui_privilege_service.dart';

class UiVisible extends StatelessWidget {
  const UiVisible({
    required this.service,
    required this.privilege,
    required this.child,
    this.replacement = const SizedBox.shrink(),
    super.key,
  });

  final UiPrivilegeService service;
  final String privilege;
  final Widget child;
  final Widget replacement;

  @override
  Widget build(BuildContext context) => AnimatedBuilder(
    animation: service,
    builder: (context, _) => service.visible(privilege) ? child : replacement,
  );
}

class UiEnabledBuilder extends StatelessWidget {
  const UiEnabledBuilder({
    required this.service,
    required this.privilege,
    required this.builder,
    this.businessDisabled = false,
    this.replacement = const SizedBox.shrink(),
    super.key,
  });

  final UiPrivilegeService service;
  final String privilege;
  final bool businessDisabled;
  final Widget replacement;
  final Widget Function(BuildContext context, bool enabled) builder;

  @override
  Widget build(BuildContext context) => AnimatedBuilder(
    animation: service,
    builder: (context, _) {
      if (!service.visible(privilege)) return replacement;
      return builder(context, service.enabled(privilege) && !businessDisabled);
    },
  );
}

class UiEditableBuilder extends StatelessWidget {
  const UiEditableBuilder({
    required this.service,
    required this.privilege,
    required this.builder,
    this.businessReadOnly = false,
    this.replacement = const SizedBox.shrink(),
    super.key,
  });

  final UiPrivilegeService service;
  final String privilege;
  final bool businessReadOnly;
  final Widget replacement;
  final Widget Function(BuildContext context, bool readOnly) builder;

  @override
  Widget build(BuildContext context) => AnimatedBuilder(
    animation: service,
    builder: (context, _) {
      if (!service.visible(privilege)) return replacement;
      return builder(context, businessReadOnly || !service.editable(privilege));
    },
  );
}
