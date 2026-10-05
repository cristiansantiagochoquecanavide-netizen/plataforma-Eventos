import 'package:flutter/foundation.dart';

enum UiPrivilegeState { visible, habilitado, editable }

extension UiPrivilegeStateWire on UiPrivilegeState {
  String get wireName => switch (this) {
    UiPrivilegeState.visible => 'VISIBLE',
    UiPrivilegeState.habilitado => 'HABILITADO',
    UiPrivilegeState.editable => 'EDITABLE',
  };

  static UiPrivilegeState? fromWire(Object? value) {
    final normalized = value?.toString().trim().toUpperCase();
    return switch (normalized) {
      'VISIBLE' => UiPrivilegeState.visible,
      'HABILITADO' => UiPrivilegeState.habilitado,
      'EDITABLE' => UiPrivilegeState.editable,
      _ => null,
    };
  }
}

@immutable
class UiPrivilegeSnapshot {
  const UiPrivilegeSnapshot(this._components);

  factory UiPrivilegeSnapshot.fromJson(Map<String, dynamic> json) {
    final rawComponents = json['componentes'];
    if (rawComponents is! Map) return UiPrivilegeSnapshot.empty;

    final parsed = <String, Set<UiPrivilegeState>>{};
    rawComponents.forEach((key, value) {
      if (key is! String || value is! Map) return;
      final rawStates = value['estadosPermitidos'];
      if (rawStates is! Iterable) return;
      final states = rawStates
          .map(UiPrivilegeStateWire.fromWire)
          .whereType<UiPrivilegeState>()
          .toSet();
      if (states.isNotEmpty) parsed[key] = Set.unmodifiable(states);
    });

    return UiPrivilegeSnapshot(Map.unmodifiable(parsed));
  }

  static const empty = UiPrivilegeSnapshot(<String, Set<UiPrivilegeState>>{});

  final Map<String, Set<UiPrivilegeState>> _components;

  bool has(String key, UiPrivilegeState state) {
    final states = _components[key];
    if (states == null || !states.contains(UiPrivilegeState.visible)) {
      return false;
    }
    return switch (state) {
      UiPrivilegeState.visible => true,
      UiPrivilegeState.habilitado => states.contains(UiPrivilegeState.habilitado),
      UiPrivilegeState.editable => states.contains(UiPrivilegeState.editable),
    };
  }

  bool visible(String key) => has(key, UiPrivilegeState.visible);
  bool enabled(String key) => has(key, UiPrivilegeState.habilitado);
  bool editable(String key) => has(key, UiPrivilegeState.editable);
}

class UiPrivilegeService extends ChangeNotifier {
  UiPrivilegeSnapshot _snapshot = UiPrivilegeSnapshot.empty;
  int _requestSerial = 0;

  bool isLoading = false;
  String? error;

  bool visible(String key) => _snapshot.visible(key);
  bool enabled(String key) => _snapshot.enabled(key);
  bool editable(String key) => _snapshot.editable(key);

  Future<void> load(Future<Map<String, dynamic>> Function() request) async {
    final requestId = ++_requestSerial;
    isLoading = true;
    error = null;
    notifyListeners();

    try {
      final response = await request();
      if (requestId != _requestSerial) return;
      _snapshot = UiPrivilegeSnapshot.fromJson(response);
      error = null;
    } catch (exception) {
      if (requestId != _requestSerial) return;
      _snapshot = UiPrivilegeSnapshot.empty;
      error = exception.toString();
    } finally {
      if (requestId == _requestSerial) {
        isLoading = false;
        notifyListeners();
      }
    }
  }

  void clear() {
    _requestSerial++;
    _snapshot = UiPrivilegeSnapshot.empty;
    isLoading = false;
    error = null;
    notifyListeners();
  }
}
