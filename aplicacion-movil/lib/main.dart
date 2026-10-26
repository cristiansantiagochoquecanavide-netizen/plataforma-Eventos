import 'dart:convert';
import 'dart:async';

import 'package:app_links/app_links.dart';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;
import 'core/theme/app_theme.dart';
import 'core/ui_privileges/ui_privilege_service.dart';
import 'core/ui_privileges/ui_privilege_widgets.dart';

const _apiUrl = String.fromEnvironment(
  'API_URL',
  defaultValue: 'http://10.0.2.2:8080/api',
);

String? _required(String? value) =>
    value == null || value.trim().isEmpty ? 'Este campo es obligatorio' : null;

String? _emailValidator(String? value) =>
    value != null && value.contains('@') ? null : 'Ingresa un correo valido';

String? _passwordValidator(String? value) {
  if (value == null || value.length < 8) {
    return 'La clave debe tener al menos 8 caracteres';
  }
  if (!RegExp(r'[A-Za-z]').hasMatch(value) || !RegExp(r'\d').hasMatch(value)) {
    return 'La clave debe incluir letras y numeros';
  }
  return null;
}

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(EventosApp(session: SessionController(ApiClient(_apiUrl))));
}

class EventosApp extends StatelessWidget {
  const EventosApp({required this.session, super.key});

  final SessionController session;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Eventos',
      debugShowCheckedModeBanner: false,
      theme: appTheme(),
      home: SessionGate(session: session),
    );
  }
}

class SessionGate extends StatefulWidget {
  const SessionGate({required this.session, super.key});

  final SessionController session;

  @override
  State<SessionGate> createState() => _SessionGateState();
}

class _SessionGateState extends State<SessionGate> {
  late final Future<void> _restoreFuture;
  late final AppLinks _appLinks;
  StreamSubscription<Uri>? _linkSubscription;
  String? _recoveryToken;

  @override
  void initState() {
    super.initState();
    _restoreFuture = widget.session.restore();
    _appLinks = AppLinks();
    _listenForRecoveryLinks();
  }

  Future<void> _listenForRecoveryLinks() async {
    final initialLink = await _appLinks.getInitialLink();
    if (initialLink != null) _handleRecoveryLink(initialLink);
    _linkSubscription = _appLinks.uriLinkStream.listen(_handleRecoveryLink);
  }

  void _handleRecoveryLink(Uri link) {
    if (link.scheme != 'eventos' || link.host != 'recuperar-clave') return;
    final token = link.queryParameters['token'];
    if (token == null || token.isEmpty || !mounted) return;
    setState(() => _recoveryToken = token);
  }

  @override
  void dispose() {
    _linkSubscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<void>(
      future: _restoreFuture,
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return const Scaffold(
            backgroundColor: AppColors.forest,
            body: Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    Icons.confirmation_number_rounded,
                    color: Colors.white,
                    size: 48,
                  ),
                  SizedBox(height: 14),
                  Text(
                    'eventos.',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 28,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  SizedBox(height: 28),
                  SizedBox(
                    width: 24,
                    height: 24,
                    child: CircularProgressIndicator(
                      color: AppColors.amber,
                      strokeWidth: 3,
                    ),
                  ),
                ],
              ),
            ),
          );
        }
        return AnimatedBuilder(
          animation: widget.session,
          builder: (context, _) {
            if (_recoveryToken != null) {
              return PasswordRecoveryPage(
                session: widget.session,
                tokenInicial: _recoveryToken,
                onCompleted: () => setState(() => _recoveryToken = null),
              );
            }
            return widget.session.user == null
                ? LoginPage(session: widget.session)
                : HomeShell(session: widget.session);
          },
        );
      },
    );
  }
}

class LoginPage extends StatefulWidget {
  const LoginPage({required this.session, super.key});

  final SessionController session;

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _formKey = GlobalKey<FormState>();
  final _email = TextEditingController();
  final _password = TextEditingController();
  bool _loading = false;
  bool _showPassword = false;
  String? _error;

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await widget.session.login(_email.text, _password.text);
    } on ApiException catch (error) {
      if (mounted) setState(() => _error = error.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) => SingleChildScrollView(
            child: ConstrainedBox(
              constraints: BoxConstraints(minHeight: constraints.maxHeight),
              child: Column(
                children: [
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.fromLTRB(28, 32, 28, 72),
                    decoration: const BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                        colors: [
                          AppColors.forest,
                          AppColors.primary,
                          AppColors.accent,
                        ],
                      ),
                      borderRadius: BorderRadius.vertical(
                        bottom: Radius.circular(36),
                      ),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Container(
                              padding: const EdgeInsets.all(10),
                              decoration: const BoxDecoration(
                                color: Color(0x33ffffff),
                                shape: BoxShape.circle,
                              ),
                              child: const Icon(
                                Icons.confirmation_number_rounded,
                                color: Colors.white,
                              ),
                            ),
                            const SizedBox(width: 12),
                            const Text(
                              'eventos.',
                              style: TextStyle(
                                color: Colors.white,
                                fontSize: 24,
                                fontWeight: FontWeight.w800,
                                letterSpacing: -1.2,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 34),
                        Text(
                          'Tu acceso\na cada momento.',
                          style: Theme.of(context).textTheme.displaySmall
                              ?.copyWith(
                                color: Colors.white,
                                fontWeight: FontWeight.w800,
                                height: .95,
                                letterSpacing: -1.5,
                              ),
                        ),
                        const SizedBox(height: 12),
                        const Text(
                          'Gestiona tu cuenta de manera simple y segura.',
                          style: TextStyle(
                            color: Color(0xffc7e4d9),
                            height: 1.45,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Transform.translate(
                    offset: const Offset(0, -38),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 20),
                      child: Card(
                        elevation: 0,
                        color: Colors.white,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(24),
                        ),
                        child: Padding(
                          padding: const EdgeInsets.all(22),
                          child: Form(
                            key: _formKey,
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.stretch,
                              children: [
                                Text(
                                  'Bienvenido',
                                  style: Theme.of(context)
                                      .textTheme
                                      .headlineSmall
                                      ?.copyWith(
                                        fontWeight: FontWeight.w800,
                                        color: AppColors.ink,
                                      ),
                                ),
                                const SizedBox(height: 5),
                                const Text(
                                  'Ingresa para continuar con tu cuenta.',
                                  style: TextStyle(color: AppColors.muted),
                                ),
                                const SizedBox(height: 24),
                                TextFormField(
                                  controller: _email,
                                  keyboardType: TextInputType.emailAddress,
                                  autofillHints: const [AutofillHints.email],
                                  decoration: const InputDecoration(
                                    labelText: 'Correo',
                                    prefixIcon: Icon(
                                      Icons.alternate_email_rounded,
                                    ),
                                  ),
                                  validator: _emailValidator,
                                ),
                                const SizedBox(height: 14),
                                TextFormField(
                                  controller: _password,
                                  obscureText: !_showPassword,
                                  autofillHints: const [AutofillHints.password],
                                  decoration: InputDecoration(
                                    labelText: 'Clave',
                                    prefixIcon: const Icon(
                                      Icons.lock_outline_rounded,
                                    ),
                                    suffixIcon: IconButton(
                                      tooltip: _showPassword
                                          ? 'Ocultar clave'
                                          : 'Mostrar clave',
                                      onPressed: () => setState(
                                        () => _showPassword = !_showPassword,
                                      ),
                                      icon: Icon(
                                        _showPassword
                                            ? Icons.visibility_off_rounded
                                            : Icons.visibility_rounded,
                                      ),
                                    ),
                                  ),
                                  validator: _required,
                                  onFieldSubmitted: (_) => _login(),
                                ),
                                Align(
                                  alignment: Alignment.centerRight,
                                  child: TextButton(
                                    onPressed: () => Navigator.of(context).push(
                                      MaterialPageRoute(
                                        builder: (_) => PasswordRecoveryPage(
                                          session: widget.session,
                                        ),
                                      ),
                                    ),
                                    child: const Text('Olvide mi clave'),
                                  ),
                                ),
                                if (_error != null)
                                  Container(
                                    margin: const EdgeInsets.only(bottom: 14),
                                    padding: const EdgeInsets.all(12),
                                    decoration: BoxDecoration(
                                      color: const Color(0xffffe8e4),
                                      borderRadius: BorderRadius.circular(12),
                                    ),
                                    child: Text(
                                      _error!,
                                      style: const TextStyle(
                                        color: AppColors.danger,
                                      ),
                                    ),
                                  ),
                                FilledButton.icon(
                                  onPressed: _loading ? null : _login,
                                  icon: _loading
                                      ? const SizedBox.square(
                                          dimension: 18,
                                          child: CircularProgressIndicator(
                                            strokeWidth: 2,
                                            color: Colors.white,
                                          ),
                                        )
                                      : const Icon(Icons.arrow_forward_rounded),
                                  label: Text(
                                    _loading
                                        ? 'Verificando...'
                                        : 'Entrar a mi cuenta',
                                  ),
                                ),
                                const SizedBox(height: 18),
                                const Divider(),
                                const SizedBox(height: 10),
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  children: [
                                    const Text(
                                      'Aun no tienes cuenta? ',
                                      style: TextStyle(color: AppColors.muted),
                                    ),
                                    TextButton(
                                      onPressed: () =>
                                          Navigator.of(context).push(
                                            MaterialPageRoute(
                                              builder: (_) => RegisterPage(
                                                session: widget.session,
                                              ),
                                            ),
                                          ),
                                      child: const Text('Crear cuenta'),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class RegisterPage extends StatefulWidget {
  const RegisterPage({required this.session, super.key});

  final SessionController session;

  @override
  State<RegisterPage> createState() => _RegisterPageState();
}

class _RegisterPageState extends State<RegisterPage> {
  final _formKey = GlobalKey<FormState>();
  final _names = TextEditingController();
  final _lastNames = TextEditingController();
  final _email = TextEditingController();
  final _password = TextEditingController();
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _names.dispose();
    _lastNames.dispose();
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _register() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await widget.session.register(
        _names.text,
        _lastNames.text,
        _email.text,
        _password.text,
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Cuenta creada. Ya puedes iniciar sesion.'),
          ),
        );
        Navigator.pop(context);
      }
    } on ApiException catch (error) {
      if (mounted) setState(() => _error = error.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Crear cuenta')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(24),
          children: [
            Text(
              'Registro de participante',
              style: Theme.of(
                context,
              ).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 8),
            const Text(
              'Tu cuenta recibira el rol PARTICIPANTE automaticamente.',
            ),
            const SizedBox(height: 24),
            Form(
              key: _formKey,
              child: Column(
                children: [
                  TextFormField(
                    controller: _names,
                    decoration: const InputDecoration(labelText: 'Nombres'),
                    validator: _required,
                  ),
                  const SizedBox(height: 14),
                  TextFormField(
                    controller: _lastNames,
                    decoration: const InputDecoration(labelText: 'Apellidos'),
                    validator: _required,
                  ),
                  const SizedBox(height: 14),
                  TextFormField(
                    controller: _email,
                    keyboardType: TextInputType.emailAddress,
                    decoration: const InputDecoration(labelText: 'Correo'),
                    validator: _emailValidator,
                  ),
                  const SizedBox(height: 14),
                  TextFormField(
                    controller: _password,
                    obscureText: true,
                    decoration: const InputDecoration(labelText: 'Clave'),
                    validator: _passwordValidator,
                  ),
                  if (_error != null)
                    Padding(
                      padding: const EdgeInsets.only(top: 14),
                      child: Text(
                        _error!,
                        style: const TextStyle(color: Color(0xffa32c20)),
                      ),
                    ),
                  const SizedBox(height: 22),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton(
                      onPressed: _loading ? null : _register,
                      child: Text(_loading ? 'Creando...' : 'Crear cuenta'),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class PasswordRecoveryPage extends StatefulWidget {
  const PasswordRecoveryPage({
    required this.session,
    this.tokenInicial,
    this.onCompleted,
    super.key,
  });

  final SessionController session;
  final String? tokenInicial;
  final VoidCallback? onCompleted;

  @override
  State<PasswordRecoveryPage> createState() => _PasswordRecoveryPageState();
}

class _PasswordRecoveryPageState extends State<PasswordRecoveryPage> {
  final _email = TextEditingController();
  final _token = TextEditingController();
  final _password = TextEditingController();
  final _passwordConfirmation = TextEditingController();
  bool _requested = false;
  bool _loading = false;
  bool _showPassword = false;
  String? _message;

  @override
  void initState() {
    super.initState();
    if (widget.tokenInicial != null && widget.tokenInicial!.isNotEmpty) {
      _token.text = widget.tokenInicial!;
      _requested = true;
    }
  }

  @override
  void dispose() {
    _email.dispose();
    _token.dispose();
    _password.dispose();
    _passwordConfirmation.dispose();
    super.dispose();
  }

  Future<void> _request() async {
    if (_emailValidator(_email.text) != null) {
      setState(() => _message = 'Ingresa un correo valido.');
      return;
    }
    setState(() {
      _loading = true;
      _message = null;
    });
    try {
      await widget.session.requestPasswordRecovery(_email.text);
      if (mounted) {
        setState(() {
          _requested = true;
          _message = 'Si la cuenta existe, enviamos un enlace de recuperacion.';
        });
      }
    } on ApiException catch (error) {
      if (mounted) setState(() => _message = error.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _reset() async {
    if (_token.text.trim().isEmpty ||
        _passwordValidator(_password.text) != null ||
        _password.text != _passwordConfirmation.text) {
      setState(
        () => _message = 'Ingresa un token y dos claves iguales y validas.',
      );
      return;
    }
    setState(() {
      _loading = true;
      _message = null;
    });
    try {
      await widget.session.resetPassword(_token.text.trim(), _password.text);
      await widget.session.logout();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Clave restablecida. Ya puedes iniciar sesion.'),
          ),
        );
        widget.onCompleted?.call();
        if (Navigator.of(context).canPop()) Navigator.pop(context);
      }
    } on ApiException catch (error) {
      if (mounted) setState(() => _message = error.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Recuperar clave')),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          Text(
            _requested ? 'Restablecer clave' : 'Solicitar recuperacion',
            style: Theme.of(
              context,
            ).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 12),
          if (!_requested) ...[
            const Text('Escribe tu correo y revisa el enlace enviado.'),
            const SizedBox(height: 20),
            TextField(
              controller: _email,
              keyboardType: TextInputType.emailAddress,
              decoration: const InputDecoration(labelText: 'Correo'),
            ),
            const SizedBox(height: 20),
            FilledButton(
              onPressed: _loading ? null : _request,
              child: Text(_loading ? 'Enviando...' : 'Solicitar enlace'),
            ),
            TextButton(
              onPressed: () => setState(() => _requested = true),
              child: const Text('Ya tengo un token'),
            ),
          ] else ...[
            Text(
              widget.tokenInicial == null
                  ? 'Pega el token recibido y define una nueva clave.'
                  : 'Define y confirma tu nueva clave.',
            ),
            const SizedBox(height: 20),
            TextField(
              controller: _token,
              readOnly: widget.tokenInicial != null,
              decoration: InputDecoration(
                labelText: 'Token de recuperacion',
                suffixIcon: widget.tokenInicial == null
                    ? null
                    : const Icon(Icons.mark_email_read_outlined),
              ),
            ),
            const SizedBox(height: 14),
            TextField(
              controller: _password,
              obscureText: !_showPassword,
              autofillHints: const [AutofillHints.newPassword],
              decoration: InputDecoration(
                labelText: 'Nueva clave',
                suffixIcon: IconButton(
                  tooltip: _showPassword ? 'Ocultar clave' : 'Mostrar clave',
                  onPressed: () =>
                      setState(() => _showPassword = !_showPassword),
                  icon: Icon(
                    _showPassword
                        ? Icons.visibility_off_rounded
                        : Icons.visibility_rounded,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 14),
            TextField(
              controller: _passwordConfirmation,
              obscureText: !_showPassword,
              autofillHints: const [AutofillHints.newPassword],
              decoration: const InputDecoration(
                labelText: 'Confirmar nueva clave',
              ),
            ),
            const SizedBox(height: 20),
            FilledButton(
              onPressed: _loading ? null : _reset,
              child: Text(_loading ? 'Restableciendo...' : 'Restablecer clave'),
            ),
          ],
          if (_message != null)
            Padding(
              padding: const EdgeInsets.only(top: 16),
              child: Text(_message!),
            ),
        ],
      ),
    );
  }
}

class ProfilePage extends StatefulWidget {
  const ProfilePage({required this.session, super.key});

  final SessionController session;

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class HomeShell extends StatefulWidget {
  const HomeShell({required this.session, super.key});

  final SessionController session;

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int _currentIndex = 0;

  @override
  Widget build(BuildContext context) {
    if (widget.session.organizationsLoaded &&
        widget.session.organizations.isEmpty) {
      return OrganizationRequiredPage(session: widget.session);
    }
    return AnimatedBuilder(
      animation: widget.session.uiPrivileges,
      builder: (context, _) {
        final privileges = widget.session.uiPrivileges;
        final destinations = <({NavigationDestination destination, Widget page})>[];
        if (privileges.visible('events.search.menu')) {
          destinations.add((
            destination: const NavigationDestination(
              icon: Icon(Icons.event_outlined),
              selectedIcon: Icon(Icons.event_rounded),
              label: 'Eventos',
            ),
            page: EventsPage(
              session: widget.session,
              onGoToInscriptions: () {
                final index = destinations.indexWhere(
                  (item) => item.page is MyInscriptionsPage,
                );
                if (index >= 0) setState(() => _currentIndex = index);
              },
            ),
          ));
        }
        if (privileges.visible('inscriptions.list')) {
          if (widget.session.puedeConsultarHistorialParticipacion) {
            destinations.add((
              destination: const NavigationDestination(
                icon: Icon(Icons.confirmation_number_outlined),
                selectedIcon: Icon(Icons.confirmation_number_rounded),
                label: 'Inscripciones',
              ),
              page: MyInscriptionsPage(session: widget.session),
            ));
          }
          destinations.add((
            destination: const NavigationDestination(
              icon: Icon(Icons.history_outlined),
              selectedIcon: Icon(Icons.history_rounded),
              label: 'Mi historial',
            ),
            page: ParticipationHistoryPage(session: widget.session),
          ));
        }
        if (privileges.visible('staff-applications.menu') ||
            privileges.visible('staff-assignments.menu')) {
          destinations.add((
            destination: const NavigationDestination(
              icon: Icon(Icons.assignment_ind_outlined),
              selectedIcon: Icon(Icons.assignment_ind_rounded),
              label: 'Staff',
            ),
            page: StaffPage(session: widget.session),
          ));
        }
        if (widget.session.puedeRegistrarAsistencia) {
          destinations.add((
            destination: const NavigationDestination(
              icon: Icon(Icons.how_to_reg_outlined),
              selectedIcon: Icon(Icons.how_to_reg_rounded),
              label: 'Asistencia',
            ),
            page: AttendancePage(session: widget.session),
          ));
        }
        if (privileges.visible('profile.menu')) {
          destinations.add((
            destination: const NavigationDestination(
              icon: Icon(Icons.person_outline_rounded),
              selectedIcon: Icon(Icons.person_rounded),
              label: 'Mi Perfil',
            ),
            page: ProfilePage(session: widget.session),
          ));
        }
        destinations.add((
          destination: const NavigationDestination(
            icon: Icon(Icons.help_outline_rounded),
            selectedIcon: Icon(Icons.help_rounded),
            label: 'Ayuda',
          ),
          page: MobileHelpPage(session: widget.session),
        ));

        final selectedIndex = destinations.isEmpty
            ? 0
            : _currentIndex.clamp(0, destinations.length - 1);
        if (selectedIndex != _currentIndex) {
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (mounted) setState(() => _currentIndex = selectedIndex);
          });
        }

        return Scaffold(
          body: Column(
            children: [
              OrganizationSwitcher(session: widget.session),
              if (privileges.isLoading) const LinearProgressIndicator(minHeight: 2),
              Expanded(
                child: destinations.isEmpty
                    ? const Center(
                        child: Padding(
                          padding: EdgeInsets.all(24),
                          child: Text(
                            'No tienes privilegios de interfaz para esta organización.',
                            textAlign: TextAlign.center,
                          ),
                        ),
                      )
                    : IndexedStack(
                        index: selectedIndex,
                        children: destinations.map((item) => item.page).toList(),
                      ),
              ),
            ],
          ),
          bottomNavigationBar: destinations.length < 2
              ? null
              : NavigationBar(
                  selectedIndex: selectedIndex,
                  onDestinationSelected: (index) => setState(() => _currentIndex = index),
                  destinations: destinations.map((item) => item.destination).toList(),
                ),
        );
      },
    );
  }
}

class MobileHelpPage extends StatelessWidget {
  const MobileHelpPage({required this.session, super.key});

  final SessionController session;

  @override
  Widget build(BuildContext context) {
    const guias = <({String titulo, String descripcion, List<String> pasos})>[
      (
        titulo: 'Organización activa',
        descripcion: 'La organización elegida limita los eventos, inscripciones y permisos que puedes consultar.',
        pasos: ['Elige la organización desde el selector superior.', 'Si no aparece información, comprueba filtros y membresía activa.'],
      ),
      (
        titulo: 'Eventos e inscripciones',
        descripcion: 'Busca eventos publicados y revisa el estado de tus inscripciones.',
        pasos: ['Actualiza la lista si acabas de cambiar de organización.', 'Abre un evento para revisar sus detalles y opciones disponibles.'],
      ),
      (
        titulo: 'Postulaciones de staff',
        descripcion: 'Envía postulaciones y revisa asignaciones de trabajo desde la sección Staff.',
        pasos: ['Selecciona una convocatoria abierta.', 'Completa la motivación y envía.', 'Consulta cambios de estado desde la misma sección.'],
      ),
      (
        titulo: 'Perfil y permisos',
        descripcion: 'La aplicación muestra solo acciones habilitadas para tu grupo en la organización activa.',
        pasos: ['Si una opción no aparece, solicita acceso al administrador.', 'La ayuda no cambia los permisos de la cuenta.'],
      ),
    ];

    return Scaffold(
      appBar: AppBar(
        title: const Text('Ayuda y guías'),
        actions: [
          IconButton(
            onPressed: session.logout,
            tooltip: 'Cerrar sesión',
            icon: const Icon(Icons.logout_rounded),
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          const Text(
            'Respuestas rápidas para usar la plataforma',
            style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 8),
          const Text('Tus datos y permisos corresponden a la organización seleccionada.'),
          const SizedBox(height: 12),
          for (final guia in guias)
            Card(
              child: ExpansionTile(
                leading: const Icon(Icons.info_outline_rounded),
                title: Text(guia.titulo),
                subtitle: Text(guia.descripcion),
                children: [
                  for (final paso in guia.pasos)
                    ListTile(
                      dense: true,
                      leading: const Icon(Icons.check_circle_outline, size: 18),
                      title: Text(paso),
                    ),
                ],
              ),
            ),
        ],
      ),
    );
  }
}

class AttendancePage extends StatefulWidget {
  const AttendancePage({required this.session, super.key});
  final SessionController session;

  @override
  State<AttendancePage> createState() => _AttendancePageState();
}

class _AttendancePageState extends State<AttendancePage> {
  final _idInscripcion = TextEditingController();
  bool _sending = false;
  String? _error;
  AttendanceModel? _attendance;

  String _formatDate(DateTime value) {
    final date = value.toLocal();
    String twoDigits(int part) => part.toString().padLeft(2, '0');
    return '${twoDigits(date.day)}/${twoDigits(date.month)}/${date.year} ${twoDigits(date.hour)}:${twoDigits(date.minute)}';
  }

  @override
  void dispose() {
    _idInscripcion.dispose();
    super.dispose();
  }

  Future<void> _checkIn() async {
    final id = int.tryParse(_idInscripcion.text.trim());
    if (id == null || id < 1) {
      setState(() => _error = 'Ingresa un ID de inscripción válido.');
      return;
    }
    setState(() { _sending = true; _error = null; _attendance = null; });
    try {
      final response = await widget.session.registrarAsistencia(id);
      if (mounted) setState(() => _attendance = response);
    } on ApiException catch (error) {
      final message = switch (error.status) {
        401 => 'Tu sesión expiró. Inicia sesión nuevamente.',
        403 => 'No tienes permiso para registrar asistencias.',
        404 => 'No se puede registrar la asistencia.',
        409 => 'Esta asistencia ya fue registrada.',
        400 => 'No se puede registrar la asistencia.',
        _ => error.status != null && error.status! >= 500
            ? 'No fue posible registrar la asistencia. Intenta nuevamente.'
            : error.message,
      };
      if (mounted) setState(() => _error = message);
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Registrar asistencia')),
    body: ListView(padding: const EdgeInsets.all(20), children: [
      const Text('Ingresa el ID de inscripción validado por el proceso de acceso.', style: TextStyle(color: AppColors.muted)),
      const SizedBox(height: 16),
      TextField(controller: _idInscripcion, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: 'ID de inscripción', prefixIcon: Icon(Icons.confirmation_number_outlined))),
      const SizedBox(height: 12),
      FilledButton.icon(onPressed: _sending ? null : _checkIn, icon: _sending ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white)) : const Icon(Icons.how_to_reg_rounded), label: Text(_sending ? 'Registrando...' : 'Registrar asistencia')),
      if (_error != null) ...[const SizedBox(height: 16), Text(_error!, key: const Key('attendance-error'), style: const TextStyle(color: AppColors.danger))],
      if (_attendance != null) ...[
        const SizedBox(height: 20),
        Card(child: Padding(padding: const EdgeInsets.all(16), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          const Text('Asistencia registrada correctamente.', style: TextStyle(fontWeight: FontWeight.w700)),
          const SizedBox(height: 12),
          Text('Participante: ${_attendance!.participante}'),
          Text('Evento: ${_attendance!.nombreEvento}'),
          Text('Fecha y hora: ${_formatDate(_attendance!.fechaHora)}'),
          Text('Registrado por: ${_attendance!.nombreRegistradoPor}'),
        ]))),
      ],
    ]),
  );
}

class StaffPage extends StatefulWidget {
  const StaffPage({required this.session, super.key});

  final SessionController session;

  @override
  State<StaffPage> createState() => _StaffPageState();
}

class _StaffPageState extends State<StaffPage> {
  final _formKey = GlobalKey<FormState>();
  final _convocatoriaId = TextEditingController();
  final _motivacion = TextEditingController();
  List<PostulacionStaffModel> _postulaciones = [];
  List<AsignacionStaffModel> _asignaciones = [];
  bool _loading = true;
  bool _sending = false;
  String? _error;
  int _organizationVersion = 0;

  @override
  void initState() {
    super.initState();
    _organizationVersion = widget.session.organizationVersion;
    widget.session.addListener(_onOrganizationChanged);
    _loadStaff();
  }

  @override
  void dispose() {
    widget.session.removeListener(_onOrganizationChanged);
    _convocatoriaId.dispose();
    _motivacion.dispose();
    super.dispose();
  }

  void _onOrganizationChanged() {
    if (!mounted ||
        _organizationVersion == widget.session.organizationVersion) {
      return;
    }
    _organizationVersion = widget.session.organizationVersion;
    setState(() {
      _postulaciones = [];
      _asignaciones = [];
    });
    if (widget.session.activeOrganization != null) _loadStaff();
  }

  Future<void> _loadStaff() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final result = await Future.wait([
        widget.session.listarMisPostulaciones(),
        widget.session.listarMisAsignaciones(),
      ]);
      if (!mounted) return;
      setState(() {
        _postulaciones = result[0] as List<PostulacionStaffModel>;
        _asignaciones = result[1] as List<AsignacionStaffModel>;
        _loading = false;
      });
    } on ApiException catch (error) {
      if (mounted) {
        setState(() {
          _error = error.message;
          _loading = false;
        });
      }
    }
  }

  Future<void> _sendApplication() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _sending = true;
      _error = null;
    });
    try {
      await widget.session.crearPostulacionStaff(
        int.parse(_convocatoriaId.text.trim()),
        _motivacion.text.trim(),
      );
      if (!mounted) return;
      _motivacion.clear();
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Postulacion enviada.')));
      await _loadStaff();
    } on ApiException catch (error) {
      if (mounted) setState(() => _error = error.message);
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  String _formatDate(DateTime? date) {
    if (date == null) return 'Sin turno definido';
    return '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year} ${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Staff'),
        actions: [
          IconButton(
            onPressed: _loading ? null : _loadStaff,
            icon: const Icon(Icons.refresh_rounded),
            tooltip: 'Actualizar staff',
          ),
        ],
      ),
      body: RefreshIndicator(onRefresh: _loadStaff, child: _buildBody()),
    );
  }

  Widget _buildBody() {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null && _postulaciones.isEmpty && _asignaciones.isEmpty) {
      return ListView(
        padding: const EdgeInsets.all(24),
        children: [
          const Icon(
            Icons.error_outline_rounded,
            size: 48,
            color: AppColors.danger,
          ),
          const SizedBox(height: 12),
          Text(
            _error!,
            textAlign: TextAlign.center,
            style: const TextStyle(color: AppColors.danger),
          ),
          const SizedBox(height: 16),
          FilledButton(onPressed: _loadStaff, child: const Text('Reintentar')),
        ],
      );
    }
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 32),
      children: [
        if (widget.session.uiPrivileges.visible('staff-applications.menu')) ...[
          _buildApplicationForm(),
          const SizedBox(height: 24),
          _buildApplicationsSection(),
          const SizedBox(height: 24),
        ],
        UiVisible(
          service: widget.session.uiPrivileges,
          privilege: 'staff-assignments.menu',
          child: _buildAssignmentsSection(),
        ),
        if (_error != null) ...[
          const SizedBox(height: 16),
          Text(_error!, style: const TextStyle(color: AppColors.danger)),
        ],
      ],
    );
  }

  Widget _buildApplicationForm() => UiEditableBuilder(
    service: widget.session.uiPrivileges,
    privilege: 'staff-applications.form',
    builder: (context, readOnly) => Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Aplicar a convocatoria',
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 8),
              const Text(
                'Ingresa el ID de la convocatoria para enviar tu postulación.',
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _convocatoriaId,
                readOnly: readOnly,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(
                  labelText: 'ID de convocatoria',
                ),
                validator: (value) {
                  final error = _required(value);
                  if (error != null) return error;
                  return int.tryParse(value!.trim()) != null &&
                          int.parse(value.trim()) > 0
                      ? null
                      : 'Ingresa un ID valido';
                },
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _motivacion,
                readOnly: readOnly,
                maxLength: 1000,
                maxLines: 3,
                decoration: const InputDecoration(
                  labelText: 'Motivacion (opcional)',
                ),
              ),
              const SizedBox(height: 8),
              UiEnabledBuilder(
                service: widget.session.uiPrivileges,
                privilege: 'staff-applications.create',
                businessDisabled: readOnly || _sending,
                builder: (context, enabled) => SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    onPressed: enabled ? _sendApplication : null,
                    icon: _sending
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Colors.white,
                            ),
                          )
                        : const Icon(Icons.send_rounded),
                    label: Text(_sending ? 'Enviando...' : 'Enviar postulación'),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    ),
  );

  Widget _buildApplicationsSection() => _section(
    'Mis postulaciones',
    _postulaciones.isEmpty
        ? const Text('Aun no tienes postulaciones.')
        : Column(
            children: _postulaciones
                .map(
                  (item) => ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.assignment_outlined),
                    title: Text('Convocatoria #${item.idConvocatoria}'),
                    subtitle: Text(
                      item.motivacion?.isNotEmpty == true
                          ? item.motivacion!
                          : 'Sin motivacion',
                    ),
                    trailing: _status(item.estado),
                  ),
                )
                .toList(),
          ),
  );

  Widget _buildAssignmentsSection() => _section(
    'Mis asignaciones',
    _asignaciones.isEmpty
        ? const Text('Aun no tienes asignaciones.')
        : Column(
            children: _asignaciones
                .map(
                  (item) => Card(
                    margin: const EdgeInsets.only(top: 10),
                    child: ListTile(
                      contentPadding: const EdgeInsets.all(8),
                      leading: const Icon(Icons.work_outline_rounded),
                      title: Text(item.tarea),
                      subtitle: Text(
                        'Turno: ${_formatDate(item.turnoInicio)} - ${item.turnoFin == null ? 'sin fin' : _formatDate(item.turnoFin)}',
                      ),
                      trailing: _status(item.estado),
                    ),
                  ),
                )
                .toList(),
          ),
  );

  Widget _section(String title, Widget child) => Card(
    child: Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 8),
          child,
        ],
      ),
    ),
  );

  Widget _status(String value) => Chip(label: Text(value));
}

class OrganizationSwitcher extends StatelessWidget {
  const OrganizationSwitcher({required this.session, super.key});

  final SessionController session;

  @override
  Widget build(BuildContext context) => AnimatedBuilder(
    animation: session,
    builder: (context, _) {
      if (!session.organizationsLoaded) {
        return const LinearProgressIndicator(minHeight: 2);
      }
      final active = session.activeOrganization;
      return Material(
        color: AppColors.canvas,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 6),
          child: Row(
            children: [
              const Icon(Icons.business_outlined, size: 20),
              const SizedBox(width: 8),
              const Text('Organizacion activa:'),
              const SizedBox(width: 8),
              Expanded(
                child: DropdownButton<int>(
                  isExpanded: true,
                  value: active?.id,
                  underline: const SizedBox.shrink(),
                  items: session.organizations
                      .map(
                        (organization) => DropdownMenuItem<int>(
                          value: organization.id,
                          child: Text(organization.name),
                        ),
                      )
                      .toList(),
                  onChanged: (id) {
                    if (id != null) session.selectOrganization(id);
                  },
                ),
              ),
            ],
          ),
        ),
      );
    },
  );
}

class OrganizationRequiredPage extends StatelessWidget {
  const OrganizationRequiredPage({required this.session, super.key});

  final SessionController session;

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Text('Organizacion requerida'),
      actions: [
        IconButton(
          onPressed: session.logout,
          icon: const Icon(Icons.logout_rounded),
          tooltip: 'Cerrar sesion',
        ),
      ],
    ),
    body: const Center(
      child: Padding(
        padding: EdgeInsets.all(24),
        child: Text(
          'Tu usuario no pertenece a ninguna organizacion activa. Solicita una membresia para continuar.',
          textAlign: TextAlign.center,
        ),
      ),
    ),
  );
}

// Pantalla del CU7: consulta eventos, busca por nombre, filtra por categoria y abre el detalle (CU11).
class EventsPage extends StatefulWidget {
  const EventsPage({required this.session, this.onGoToInscriptions, super.key});

  final SessionController session;
  final VoidCallback? onGoToInscriptions;

  @override
  State<EventsPage> createState() => _EventsPageState();
}

class _EventsPageState extends State<EventsPage> {
  final _search = TextEditingController();
  List<EventModel> _events = [];
  List<String> _categories = [];
  String _category = '';
  bool _loading = true;
  bool _searched = false;
  String? _error;
  int _organizationVersion = 0;

  @override
  void initState() {
    super.initState();
    _loading = false;
    _organizationVersion = widget.session.organizationVersion;
    widget.session.addListener(_onOrganizationChanged);
  }

  @override
  void dispose() {
    widget.session.removeListener(_onOrganizationChanged);
    _search.dispose();
    super.dispose();
  }

  void _onOrganizationChanged() {
    if (!mounted ||
        _organizationVersion == widget.session.organizationVersion) {
      return;
    }
    _organizationVersion = widget.session.organizationVersion;
    setState(() {
      _events = [];
      _categories = [];
      _searched = false;
    });
    if (widget.session.activeOrganization != null) _loadEvents();
  }

  Future<void> _loadEvents() async {
    setState(() => _searched = true);
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final events = await widget.session.listEvents(_search.text, _category);
      if (!mounted) return;
      setState(() {
        _events = events;
        _categories = events.map((event) => event.category).toSet().toList()
          ..sort();
        _loading = false;
      });
    } on ApiException catch (error) {
      if (mounted) {
        setState(() {
          _error = error.message;
          _events = [];
          _loading = false;
        });
      }
    }
  }

  void _clearFilters() {
    _search.clear();
    setState(() {
      _category = '';
      _events = [];
      _searched = false;
    });
  }

  void _showDetail(EventModel event) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (context) => EventDetailModal(
        event: event,
        session: widget.session,
        onInscribed: () {
          _loadEvents();
          widget.onGoToInscriptions?.call();
        },
      ),
    );
  }

  String _formatDate(DateTime date) =>
      '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year} ${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Eventos disponibles'),
        actions: [
          if (widget.session.uiPrivileges.visible('inscriptions.list'))
            IconButton(
              onPressed: () {
                if (widget.onGoToInscriptions != null) {
                  widget.onGoToInscriptions!();
                } else {
                  Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (_) => MyInscriptionsPage(session: widget.session),
                    ),
                  );
                }
              },
              icon: const Icon(Icons.confirmation_number_outlined),
              tooltip: 'Mis inscripciones',
            ),
          if (widget.session.uiPrivileges.visible('profile.menu'))
            IconButton(
              onPressed: () => Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (_) => ProfilePage(session: widget.session),
                ),
              ),
              icon: const Icon(Icons.person_outline_rounded),
              tooltip: 'Mi perfil',
            ),
          IconButton(
            onPressed: () => widget.session.logout(),
            icon: const Icon(Icons.logout_rounded),
            tooltip: 'Cerrar sesion',
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadEvents,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 30),
          children: [
            const Text(
              'Consulta y busqueda de eventos',
              style: TextStyle(color: AppColors.muted),
            ),
            const SizedBox(height: 16),
            UiEditableBuilder(
              service: widget.session.uiPrivileges,
              privilege: 'events.filters',
              builder: (context, readOnly) => Column(
                children: [
                  TextField(
                    controller: _search,
                    readOnly: readOnly,
                    onSubmitted: readOnly ? null : (_) => _loadEvents(),
                    decoration: InputDecoration(
                      labelText: 'Buscar por titulo o nombre',
                      prefixIcon: const Icon(Icons.search_rounded),
                      suffixIcon: IconButton(
                        onPressed: readOnly ? null : _loadEvents,
                        icon: const Icon(Icons.arrow_forward_rounded),
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String>(
                    initialValue: _category.isEmpty ? null : _category,
                    decoration: const InputDecoration(
                      labelText: 'Filtrar por categoria',
                      prefixIcon: Icon(Icons.category_outlined),
                    ),
                    items: [
                      const DropdownMenuItem(
                        value: '',
                        child: Text('Todas las categorias'),
                      ),
                      ..._categories.map(
                        (category) => DropdownMenuItem(
                          value: category,
                          child: Text(category),
                        ),
                      ),
                    ],
                    onChanged: readOnly
                        ? null
                        : (value) {
                            setState(() => _category = value ?? '');
                            _loadEvents();
                          },
                  ),
                  Align(
                    alignment: Alignment.centerRight,
                    child: TextButton(
                      onPressed: readOnly ? null : _clearFilters,
                      child: const Text('Limpiar filtros'),
                    ),
                  ),
                ],
              ),
            ),
            if (_error != null)
              Text(_error!, style: const TextStyle(color: AppColors.danger)),
            if (_loading)
              const Padding(
                padding: EdgeInsets.all(30),
                child: Center(child: CircularProgressIndicator()),
              ),
            if (!_loading && _searched && _events.isEmpty)
              const Padding(
                padding: EdgeInsets.all(30),
                child: Center(
                  child: Text(
                    'No se encontraron eventos con los criterios seleccionados.',
                    textAlign: TextAlign.center,
                  ),
                ),
              ),
            if (widget.session.uiPrivileges.visible('events.list'))
              ..._events.map(
              (event) => Card(
                margin: const EdgeInsets.only(bottom: 12),
                child: InkWell(
                  borderRadius: BorderRadius.circular(18),
                  onTap: () => _showDetail(event),
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Expanded(
                              child: Text(
                                event.name,
                                style: const TextStyle(
                                  fontWeight: FontWeight.w800,
                                  fontSize: 17,
                                ),
                              ),
                            ),
                            const Icon(Icons.chevron_right_rounded),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Wrap(
                          spacing: 8,
                          children: [
                            Chip(label: Text(event.category)),
                            Text(
                              _formatDate(event.start),
                              style: const TextStyle(color: AppColors.muted),
                            ),
                          ],
                        ),
                        const SizedBox(height: 4),
                        Text(
                          event.place.isEmpty
                              ? 'Sin lugar asignado'
                              : event.place,
                          style: const TextStyle(color: AppColors.muted),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// Modal interactivo de Detalle de Evento con Aforo y Botón de Inscripción (CU11)
class EventDetailModal extends StatefulWidget {
  const EventDetailModal({
    required this.event,
    required this.session,
    this.onInscribed,
    super.key,
  });

  final EventModel event;
  final SessionController session;
  final VoidCallback? onInscribed;

  @override
  State<EventDetailModal> createState() => _EventDetailModalState();
}

class _EventDetailModalState extends State<EventDetailModal> {
  DisponibilidadModel? _disponibilidad;
  bool _loading = true;
  bool _inscribiendo = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadDisponibilidad();
  }

  Future<void> _loadDisponibilidad() async {
    try {
      final disp = await widget.session.obtenerDisponibilidad(widget.event.id);
      if (mounted) {
        setState(() {
          _disponibilidad = disp;
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _inscribir() async {
    setState(() {
      _inscribiendo = true;
      _error = null;
    });
    try {
      final ins = await widget.session.inscribirAEvento(widget.event.id);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('¡Inscrito con éxito! Código: ${ins.codigoQr}'),
          backgroundColor: AppColors.forest,
        ),
      );
      await _loadDisponibilidad();
      widget.onInscribed?.call();
    } on ApiException catch (e) {
      if (mounted) {
        setState(() => _error = e.message);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.message), backgroundColor: AppColors.danger),
        );
      }
    } finally {
      if (mounted) setState(() => _inscribiendo = false);
    }
  }

  String _formatDate(DateTime date) =>
      '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year} ${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}';

  @override
  Widget build(BuildContext context) {
    final event = widget.event;
    return Padding(
      padding: EdgeInsets.only(
        left: 22,
        right: 22,
        top: 4,
        bottom: MediaQuery.of(context).viewInsets.bottom + 26,
      ),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Text(
                    event.name,
                    style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Chip(label: Text(event.category)),
              ],
            ),
            const SizedBox(height: 8),
            UiVisible(
              service: widget.session.uiPrivileges,
              privilege: 'event.summary',
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    event.description.isEmpty
                        ? 'Sin descripción'
                        : event.description,
                  ),
                  const SizedBox(height: 12),
                  const Divider(),
                  ListTile(
                    dense: true,
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.calendar_today_rounded),
                    title: Text(_formatDate(event.start)),
                    subtitle: const Text('Fecha de inicio'),
                  ),
                  if (event.end != null)
                    ListTile(
                      dense: true,
                      contentPadding: EdgeInsets.zero,
                      leading: const Icon(Icons.event_available_rounded),
                      title: Text(_formatDate(event.end!)),
                      subtitle: const Text('Fecha de finalización'),
                    ),
                  ListTile(
                    dense: true,
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.place_outlined),
                    title: Text(
                      event.place.isEmpty ? 'Sin lugar asignado' : event.place,
                    ),
                    subtitle: const Text('Lugar'),
                  ),
                  const Divider(),
                ],
              ),
            ),
            const SizedBox(height: 4),
            // Sección CU11: Aforo y Control de Cupos
            if (_loading)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 12),
                child: Center(
                  child: SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  ),
                ),
              )
            else if (_disponibilidad != null) ...[
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.canvas,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: [
                        const Icon(
                          Icons.people_alt_outlined,
                          color: AppColors.forest,
                        ),
                        const SizedBox(width: 8),
                        Text(
                          _disponibilidad!.aforoMaximo != null
                              ? 'Cupos: ${_disponibilidad!.cuposOcupados} / ${_disponibilidad!.aforoMaximo}'
                              : 'Ocupados: ${_disponibilidad!.cuposOcupados} (sin límite)',
                          style: const TextStyle(fontWeight: FontWeight.w600),
                        ),
                      ],
                    ),
                    if (_disponibilidad!.cuposDisponibles != null)
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 4,
                        ),
                        decoration: BoxDecoration(
                          color: _disponibilidad!.tieneCupo
                              ? Colors.green.shade100
                              : Colors.red.shade100,
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Text(
                          '${_disponibilidad!.cuposDisponibles} disp.',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: _disponibilidad!.tieneCupo
                                ? Colors.green.shade900
                                : Colors.red.shade900,
                          ),
                        ),
                      ),
                  ],
                ),
              ),
              if (_error != null)
                Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: Text(
                    _error!,
                    style: const TextStyle(
                      color: AppColors.danger,
                      fontSize: 13,
                    ),
                  ),
                ),
              const SizedBox(height: 12),
              if (_disponibilidad!.estaInscrito)
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.symmetric(
                    vertical: 12,
                    horizontal: 16,
                  ),
                  decoration: BoxDecoration(
                    color: Colors.green.shade50,
                    border: Border.all(color: Colors.green.shade400),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.check_circle_rounded, color: Colors.green),
                      SizedBox(width: 8),
                      Text(
                        'Ya estás inscrito a este evento',
                        style: TextStyle(
                          color: Colors.green,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                )
              else
                UiEnabledBuilder(
                  service: widget.session.uiPrivileges,
                  privilege: 'event.registration.create',
                  businessDisabled: !_disponibilidad!.tieneCupo || _inscribiendo,
                  builder: (context, enabled) => SizedBox(
                    width: double.infinity,
                    child: FilledButton.icon(
                      onPressed: enabled ? _inscribir : null,
                      icon: _inscribiendo
                          ? const SizedBox(
                              width: 18,
                              height: 18,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                color: Colors.white,
                              ),
                            )
                          : const Icon(Icons.how_to_reg_rounded),
                      label: Text(
                        _inscribiendo
                            ? 'Inscribiendo...'
                            : (_disponibilidad!.tieneCupo
                                  ? 'Inscribirme al evento'
                                  : 'Cupos agotados'),
                      ),
                    ),
                  ),
                ),
            ],
          ],
        ),
      ),
    );
  }
}

// Pantalla de Consulta de Estado de Inscripciones (CU12) y Cancelación (CU13)
// Pantalla del CU27: historial de participación y eventos asistidos.
class ParticipationHistoryPage extends StatefulWidget {
  const ParticipationHistoryPage({required this.session, super.key});
  final SessionController session;

  @override
  State<ParticipationHistoryPage> createState() => _ParticipationHistoryPageState();
}

class _ParticipationHistoryPageState extends State<ParticipationHistoryPage> {
  List<HistorialParticipacionModel> _history = [];
  bool _loading = true;
  String? _error;
  int _organizationVersion = 0;

  @override
  void initState() {
    super.initState();
    _organizationVersion = widget.session.organizationVersion;
    widget.session.addListener(_onOrganizationChanged);
    _loadHistory();
  }

  @override
  void dispose() {
    widget.session.removeListener(_onOrganizationChanged);
    super.dispose();
  }

  void _onOrganizationChanged() {
    if (!mounted || _organizationVersion == widget.session.organizationVersion) return;
    _organizationVersion = widget.session.organizationVersion;
    setState(() => _history = []);
    if (widget.session.activeOrganization != null) _loadHistory();
  }

  Future<void> _loadHistory() async {
    setState(() { _loading = true; _error = null; });
    try {
      final history = await widget.session.listarMiHistorial();
      if (mounted) setState(() { _history = history; _loading = false; });
    } on ApiException catch (e) {
      if (mounted) setState(() { _error = e.message; _loading = false; });
    }
  }

  String _formatDate(DateTime date) => '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year}';

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Mi historial de participación'), actions: [IconButton(onPressed: _loadHistory, icon: const Icon(Icons.refresh_rounded))]),
    body: RefreshIndicator(onRefresh: _loadHistory, child: _buildBody()),
  );

  Widget _buildBody() {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) return ListView(padding: const EdgeInsets.all(24), children: [Text(_error!, textAlign: TextAlign.center, style: const TextStyle(color: AppColors.danger)), const SizedBox(height: 16), FilledButton(onPressed: _loadHistory, child: const Text('Reintentar'))]);
    if (_history.isEmpty) return ListView(padding: const EdgeInsets.all(32), children: [const Icon(Icons.history_rounded, size: 64, color: AppColors.muted), const SizedBox(height: 16), const Text('Aún no tienes participación registrada', textAlign: TextAlign.center, style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)), const SizedBox(height: 8), const Text('Tus inscripciones y asistencias aparecerán aquí.', textAlign: TextAlign.center, style: TextStyle(color: AppColors.muted))]);

    final attended = _history.where((item) => item.asistio).length;
    return ListView(padding: const EdgeInsets.fromLTRB(16, 16, 16, 32), children: [
      Card(child: Padding(padding: const EdgeInsets.all(16), child: Row(mainAxisAlignment: MainAxisAlignment.spaceAround, children: [_metric('Registros', '${_history.length}'), _metric('Asistidos', '$attended'), _metric('Pendientes', '${_history.length - attended}')]))),
      const SizedBox(height: 12),
      ..._history.map((item) => Card(margin: const EdgeInsets.only(bottom: 12), child: ListTile(leading: CircleAvatar(backgroundColor: item.asistio ? AppColors.forest.withValues(alpha: .12) : Colors.grey.shade200, child: Icon(item.asistio ? Icons.check_rounded : Icons.event_outlined, color: item.asistio ? AppColors.forest : AppColors.muted)), title: Text(item.nombreEvento, style: const TextStyle(fontWeight: FontWeight.bold)), subtitle: Text('${_formatDate(item.fechaInicioEvento)} · ${item.lugarEvento}\n${item.asistio ? 'Asistencia registrada' : 'Sin asistencia registrada'} · ${item.estadoInscripcion}'), isThreeLine: true))),
    ]);
  }

  Widget _metric(String label, String value) => Column(children: [Text(value, style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold)), Text(label, style: const TextStyle(color: AppColors.muted, fontSize: 12))]);
}

class MyInscriptionsPage extends StatefulWidget {
  const MyInscriptionsPage({required this.session, super.key});

  final SessionController session;

  @override
  State<MyInscriptionsPage> createState() => _MyInscriptionsPageState();
}

class _MyInscriptionsPageState extends State<MyInscriptionsPage> {
  List<InscripcionModel> _inscriptions = [];
  String _selectedFilter = 'CONFIRMADA';
  bool _loading = true;
  String? _error;
  int _organizationVersion = 0;

  @override
  void initState() {
    super.initState();
    _organizationVersion = widget.session.organizationVersion;
    widget.session.addListener(_onOrganizationChanged);
    _loadInscriptions();
  }

  @override
  void dispose() {
    widget.session.removeListener(_onOrganizationChanged);
    super.dispose();
  }

  void _onOrganizationChanged() {
    if (!mounted ||
        _organizationVersion == widget.session.organizationVersion) {
      return;
    }
    _organizationVersion = widget.session.organizationVersion;
    setState(() => _inscriptions = []);
    if (widget.session.activeOrganization != null) _loadInscriptions();
  }

  Future<void> _loadInscriptions() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final list = await widget.session.listarMisInscripciones();
      if (!mounted) return;
      setState(() {
        _inscriptions = list;
        _loading = false;
      });
    } on ApiException catch (e) {
      if (mounted) {
        setState(() {
          _error = e.message;
          _loading = false;
        });
      }
    }
  }

  Future<void> _cancelarInscripcion(InscripcionModel inscripcion) async {
    final confirmar = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('¿Cancelar inscripción?'),
        content: Text(
          'Se cancelará tu inscripción al evento "${inscripcion.nombreEvento}" y se liberará el cupo para otros participantes.\n\n¿Estás seguro de continuar?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('No, mantener'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: AppColors.danger),
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Sí, cancelar inscripción'),
          ),
        ],
      ),
    );

    if (confirmar != true || !mounted) return;

    try {
      await widget.session.cancelarInscripcion(inscripcion.idInscripcion);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Tu inscripción ha sido cancelada exitosamente.'),
          backgroundColor: AppColors.forest,
        ),
      );
      await _loadInscriptions();
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.message), backgroundColor: AppColors.danger),
      );
    }
  }

  Future<void> _mostrarCredencial(InscripcionModel inscripcion) async {
    CredentialModel? credential;
    var offline = false;
    try {
      credential = await widget.session.obtenerCredencial(inscripcion.idInscripcion);
    } catch (error) {
      if (error is ApiException && error.status != null && error.status! < 500) {
        rethrow;
      }
      credential = await widget.session.credencialOffline(inscripcion.idInscripcion);
      offline = credential != null;
    }
    if (!mounted) return;
    if (offline && credential != null && DateTime.now().isAfter(credential.expiraEn)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('La credencial guardada ya venció. Conéctate para emitir un QR vigente.')),
      );
      return;
    }
    if (credential == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('No se pudo emitir ni encontrar una credencial guardada para esta inscripción.')),
      );
      return;
    }
    await showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(offline ? 'Credencial guardada · sin conexión' : 'Credencial digital'),
        content: SizedBox(
          width: 300,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(credential!.nombreEvento, textAlign: TextAlign.center,
                style: const TextStyle(fontWeight: FontWeight.w700)),
              const SizedBox(height: 4),
              Text('${credential.nombreParticipante} · ${credential.categoria}',
                textAlign: TextAlign.center, style: const TextStyle(color: AppColors.muted)),
              const SizedBox(height: 16),
              Image.memory(base64Decode(credential.qrPngBase64), width: 220, height: 220,
                errorBuilder: (_, _, _) => const Icon(Icons.qr_code_2, size: 180)),
              const SizedBox(height: 8),
              Text('Válido hasta ${_formatDate(credential.expiraEn)}',
                style: const TextStyle(fontSize: 11, color: AppColors.muted)),
              if (offline) const Padding(
                padding: EdgeInsets.only(top: 8),
                child: Text('Credencial almacenada de forma segura. Verifica su vigencia al recuperar conexión.',
                  textAlign: TextAlign.center, style: TextStyle(fontSize: 11)),
              ),
            ],
          ),
        ),
        actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cerrar'))],
      ),
    );
  }

  String _formatDate(DateTime date) =>
      '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year} ${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Mis inscripciones'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded),
            tooltip: 'Actualizar lista',
            onPressed: _loadInscriptions,
          ),
        ],
      ),
      body: RefreshIndicator(onRefresh: _loadInscriptions, child: _buildBody()),
    );
  }

  Widget _buildBody() {
    if (!widget.session.uiPrivileges.visible('inscriptions.list')) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: Text('No tienes privilegios para ver tus inscripciones.'),
        ),
      );
    }
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_error != null) {
      return ListView(
        padding: const EdgeInsets.all(24),
        children: [
          Center(
            child: Column(
              children: [
                const Icon(
                  Icons.error_outline_rounded,
                  size: 48,
                  color: AppColors.danger,
                ),
                const SizedBox(height: 12),
                Text(
                  _error!,
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: AppColors.danger),
                ),
                const SizedBox(height: 16),
                FilledButton(
                  onPressed: _loadInscriptions,
                  child: const Text('Reintentar'),
                ),
              ],
            ),
          ),
        ],
      );
    }

    if (_inscriptions.isEmpty) {
      return ListView(
        padding: const EdgeInsets.all(32),
        children: [
          Center(
            child: Column(
              children: [
                const SizedBox(height: 40),
                Icon(
                  Icons.confirmation_number_outlined,
                  size: 64,
                  color: AppColors.muted.withValues(alpha: 0.5),
                ),
                const SizedBox(height: 16),
                const Text(
                  'Aún no tienes inscripciones',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 8),
                const Text(
                  'Explora los eventos disponibles e inscríbete para ver tus confirmaciones y credenciales aquí.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: AppColors.muted),
                ),
              ],
            ),
          ),
        ],
      );
    }

    final activasCount = _inscriptions
        .where((i) => i.estado == 'CONFIRMADA')
        .length;
    final canceladasCount = _inscriptions
        .where((i) => i.estado == 'CANCELADA')
        .length;

    final filtered = _inscriptions.where((ins) {
      if (_selectedFilter == 'CONFIRMADA') return ins.estado == 'CONFIRMADA';
      if (_selectedFilter == 'CANCELADA') return ins.estado == 'CANCELADA';
      return true;
    }).toList();

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
          child: SizedBox(
            width: double.infinity,
            child: SegmentedButton<String>(
              showSelectedIcon: false,
              segments: [
                ButtonSegment(
                  value: 'CONFIRMADA',
                  label: Text('Activas ($activasCount)'),
                  icon: const Icon(Icons.check_circle_outline, size: 16),
                ),
                ButtonSegment(
                  value: 'CANCELADA',
                  label: Text('Canceladas ($canceladasCount)'),
                  icon: const Icon(Icons.cancel_outlined, size: 16),
                ),
                ButtonSegment(
                  value: 'TODAS',
                  label: Text('Todas (${_inscriptions.length})'),
                ),
              ],
              selected: {_selectedFilter},
              onSelectionChanged: (set) =>
                  setState(() => _selectedFilter = set.first),
            ),
          ),
        ),
        Expanded(
          child: filtered.isEmpty
              ? ListView(
                  padding: const EdgeInsets.all(32),
                  children: [
                    Center(
                      child: Column(
                        children: [
                          const SizedBox(height: 30),
                          Icon(
                            _selectedFilter == 'CONFIRMADA'
                                ? Icons.event_available_outlined
                                : Icons.history_rounded,
                            size: 48,
                            color: AppColors.muted.withValues(alpha: 0.5),
                          ),
                          const SizedBox(height: 12),
                          Text(
                            _selectedFilter == 'CONFIRMADA'
                                ? 'No tienes entradas activas'
                                : 'No hay inscripciones canceladas',
                            style: const TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          const SizedBox(height: 6),
                          Text(
                            _selectedFilter == 'CONFIRMADA'
                                ? 'Inscríbete a un evento en la pestaña "Eventos" para ver tu entrada y QR aquí.'
                                : 'Cuando canceles una inscripción, quedará registrada en esta sección.',
                            textAlign: TextAlign.center,
                            style: const TextStyle(
                              color: AppColors.muted,
                              fontSize: 13,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                )
              : ListView.builder(
                  padding: const EdgeInsets.fromLTRB(16, 4, 16, 32),
                  itemCount: filtered.length,
                  itemBuilder: (context, index) {
                    final ins = filtered[index];
                    final isConfirmada = ins.estado == 'CONFIRMADA';

                    return Card(
                      margin: const EdgeInsets.only(bottom: 14),
                      color: isConfirmada ? null : Colors.grey.shade50,
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Expanded(
                                  child: Text(
                                    ins.nombreEvento,
                                    style: TextStyle(
                                      fontSize: 17,
                                      fontWeight: FontWeight.w800,
                                      color: isConfirmada
                                          ? null
                                          : Colors.grey.shade700,
                                    ),
                                  ),
                                ),
                                Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 10,
                                    vertical: 4,
                                  ),
                                  decoration: BoxDecoration(
                                    color: isConfirmada
                                        ? Colors.green.shade50
                                        : Colors.grey.shade200,
                                    border: Border.all(
                                      color: isConfirmada
                                          ? Colors.green.shade400
                                          : Colors.grey.shade400,
                                    ),
                                    borderRadius: BorderRadius.circular(20),
                                  ),
                                  child: Row(
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      Icon(
                                        isConfirmada
                                            ? Icons.check_circle_outline
                                            : Icons.cancel_outlined,
                                        size: 14,
                                        color: isConfirmada
                                            ? Colors.green.shade800
                                            : Colors.grey.shade700,
                                      ),
                                      const SizedBox(width: 4),
                                      Text(
                                        ins.estado,
                                        style: TextStyle(
                                          fontSize: 12,
                                          fontWeight: FontWeight.bold,
                                          color: isConfirmada
                                              ? Colors.green.shade800
                                              : Colors.grey.shade700,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 8),
                            Wrap(
                              spacing: 8,
                              runSpacing: 4,
                              children: [
                                Chip(label: Text(ins.categoriaEvento)),
                                Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    const Icon(
                                      Icons.calendar_today_rounded,
                                      size: 14,
                                      color: AppColors.muted,
                                    ),
                                    const SizedBox(width: 4),
                                    Text(
                                      _formatDate(ins.fechaInicioEvento),
                                      style: const TextStyle(
                                        color: AppColors.muted,
                                        fontSize: 13,
                                      ),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                            const SizedBox(height: 6),
                            Row(
                              children: [
                                const Icon(
                                  Icons.place_outlined,
                                  size: 16,
                                  color: AppColors.muted,
                                ),
                                const SizedBox(width: 4),
                                Expanded(
                                  child: Text(
                                    ins.lugarEvento,
                                    style: const TextStyle(
                                      color: AppColors.muted,
                                      fontSize: 13,
                                    ),
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 12),
                            // Código QR / Hash (CU12)
                            Container(
                              width: double.infinity,
                              padding: const EdgeInsets.symmetric(
                                horizontal: 12,
                                vertical: 8,
                              ),
                              decoration: BoxDecoration(
                                color: isConfirmada
                                    ? AppColors.canvas
                                    : Colors.grey.shade200,
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Row(
                                children: [
                                  Icon(
                                    isConfirmada
                                        ? Icons.qr_code_2_rounded
                                        : Icons.qr_code_scanner_rounded,
                                    size: 20,
                                    color: isConfirmada
                                        ? AppColors.forest
                                        : Colors.grey.shade600,
                                  ),
                                  const SizedBox(width: 8),
                                  Expanded(
                                    child: Text(
                                      isConfirmada
                                          ? 'Credencial QR firmada disponible'
                                          : 'Credencial anulada',
                                      style: TextStyle(
                                        fontFamily: 'monospace',
                                        fontWeight: FontWeight.w600,
                                        fontSize: 13,
                                        color: isConfirmada
                                            ? null
                                            : Colors.grey.shade600,
                                        decoration: isConfirmada
                                            ? null
                                            : TextDecoration.lineThrough,
                                      ),
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ),
                                  if (!isConfirmada)
                                    Text(
                                      'ANULADO',
                                      style: TextStyle(
                                        fontSize: 10,
                                        fontWeight: FontWeight.bold,
                                        color: Colors.red.shade400,
                                      ),
                                    ),
                                ],
                              ),
                            ),
                            const SizedBox(height: 6),
                            Text(
                              'Inscrito el: ${_formatDate(ins.fechaInscripcion)}',
                              style: const TextStyle(
                                color: AppColors.muted,
                                fontSize: 11,
                              ),
                            ),
                            if (isConfirmada) ...[
                              const SizedBox(height: 12),
                              Align(
                                alignment: Alignment.centerRight,
                                child: FilledButton.icon(
                                  onPressed: () => _mostrarCredencial(ins),
                                  icon: const Icon(Icons.qr_code_2_rounded),
                                  label: const Text('Ver mi credencial QR'),
                                ),
                              ),
                              const SizedBox(height: 8),
                              UiEnabledBuilder(
                                service: widget.session.uiPrivileges,
                                privilege: 'inscriptions.cancel',
                                builder: (context, enabled) => Align(
                                  alignment: Alignment.centerRight,
                                  child: OutlinedButton.icon(
                                    style: OutlinedButton.styleFrom(
                                      foregroundColor: AppColors.danger,
                                      side: const BorderSide(
                                        color: AppColors.danger,
                                      ),
                                    ),
                                    onPressed: enabled
                                        ? () => _cancelarInscripcion(ins)
                                        : null,
                                    icon: const Icon(
                                      Icons.cancel_outlined,
                                      size: 16,
                                    ),
                                    label: const Text('Cancelar inscripción'),
                                  ),
                                ),
                              ),
                            ],
                          ],
                        ),
                      ),
                    );
                  },
                ),
        ),
      ],
    );
  }
}

class _ProfilePageState extends State<ProfilePage> {
  late final TextEditingController _names;
  late final TextEditingController _lastNames;
  late final TextEditingController _email;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    final user = widget.session.user!;
    _names = TextEditingController(text: user.names);
    _lastNames = TextEditingController(text: user.lastNames);
    _email = TextEditingController(text: user.email);
  }

  @override
  void dispose() {
    _names.dispose();
    _lastNames.dispose();
    _email.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    setState(() => _saving = true);
    try {
      await widget.session.updateProfile(
        _names.text,
        _lastNames.text,
        _email.text,
      );
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('Perfil actualizado.')));
      }
    } on ApiException catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(error.message)));
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  Future<void> _changePassword() async {
    final current = TextEditingController();
    final next = TextEditingController();
    final accepted = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Actualizar clave'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: current,
              obscureText: true,
              decoration: const InputDecoration(labelText: 'Clave actual'),
            ),
            TextField(
              controller: next,
              obscureText: true,
              decoration: const InputDecoration(labelText: 'Nueva clave'),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Guardar'),
          ),
        ],
      ),
    );
    if (accepted != true) return;
    try {
      await widget.session.changePassword(current.text, next.text);
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('Clave actualizada.')));
      }
    } on ApiException catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(error.message)));
      }
    } finally {
      current.dispose();
      next.dispose();
    }
  }

  Future<void> _logout() async {
    final accepted = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Cerrar sesion'),
        content: const Text(
          'Tendras que volver a ingresar tus credenciales para continuar.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Cerrar sesion'),
          ),
        ],
      ),
    );
    if (accepted == true) await widget.session.logout();
  }

  @override
  Widget build(BuildContext context) {
    final user = widget.session.user!;
    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 18, 20, 36),
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'eventos.',
                  style: TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.w800,
                    color: AppColors.forest,
                    letterSpacing: -1.2,
                  ),
                ),
                IconButton(
                  onPressed: _logout,
                  icon: const Icon(Icons.logout_rounded),
                  tooltip: 'Cerrar sesion',
                ),
              ],
            ),
            const SizedBox(height: 18),
            Container(
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [AppColors.forest, AppColors.primary],
                ),
                borderRadius: BorderRadius.circular(24),
              ),
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Row(
                  children: [
                    CircleAvatar(
                      radius: 27,
                      backgroundColor: const Color(0x33ffffff),
                      child: Text(
                        user.names.substring(0, 1).toUpperCase(),
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 23,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                    ),
                    const SizedBox(width: 15),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'MI CUENTA',
                            style: TextStyle(
                              color: Color(0xffb9e2d5),
                              fontSize: 11,
                              fontWeight: FontWeight.w700,
                              letterSpacing: 1.1,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            'Hola, ${user.names}',
                            style: Theme.of(context).textTheme.titleLarge
                                ?.copyWith(
                                  color: Colors.white,
                                  fontWeight: FontWeight.w800,
                                ),
                          ),
                          const SizedBox(height: 6),
                          Wrap(
                            spacing: 6,
                            children: user.roles
                                .map(
                                  (role) => Chip(
                                    label: Text(role),
                                    labelStyle: const TextStyle(
                                      fontSize: 10,
                                      color: AppColors.forest,
                                      fontWeight: FontWeight.w800,
                                    ),
                                    backgroundColor: AppColors.amber,
                                    side: BorderSide.none,
                                    visualDensity: VisualDensity.compact,
                                  ),
                                )
                                .toList(),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 28),
            Text(
              'Datos personales',
              style: Theme.of(
                context,
              ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 4),
            const Text(
              'Manten tus datos actualizados para recibir comunicaciones.',
              style: TextStyle(color: AppColors.muted),
            ),
            const SizedBox(height: 16),
            UiEditableBuilder(
              service: widget.session.uiPrivileges,
              privilege: 'profile.edit',
              builder: (context, readOnly) => Card(
                elevation: 0,
                color: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(18),
                  child: Column(
                    children: [
                      TextField(
                        controller: _names,
                        readOnly: readOnly,
                        decoration: const InputDecoration(
                          labelText: 'Nombres',
                          prefixIcon: Icon(Icons.person_outline_rounded),
                        ),
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _lastNames,
                        readOnly: readOnly,
                        decoration: const InputDecoration(
                          labelText: 'Apellidos',
                          prefixIcon: Icon(Icons.badge_outlined),
                        ),
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _email,
                        readOnly: readOnly,
                        keyboardType: TextInputType.emailAddress,
                        decoration: const InputDecoration(
                          labelText: 'Correo',
                          prefixIcon: Icon(Icons.alternate_email_rounded),
                        ),
                      ),
                      const SizedBox(height: 18),
                      UiEnabledBuilder(
                        service: widget.session.uiPrivileges,
                        privilege: 'profile.save',
                        businessDisabled: readOnly || _saving,
                        builder: (context, enabled) => FilledButton.icon(
                          onPressed: enabled ? _save : null,
                          icon: const Icon(Icons.save_rounded),
                          label: Text(
                            _saving ? 'Guardando...' : 'Guardar cambios',
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(height: 24),
            Text(
              'Seguridad',
              style: Theme.of(
                context,
              ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 12),
            Card(
              elevation: 0,
              color: const Color(0xffe2f0ea),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(20),
              ),
              child: ListTile(
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: 18,
                  vertical: 8,
                ),
                leading: const CircleAvatar(
                  backgroundColor: AppColors.primary,
                  foregroundColor: Colors.white,
                  child: Icon(Icons.lock_reset_rounded),
                ),
                title: const Text(
                  'Actualizar clave',
                  style: TextStyle(fontWeight: FontWeight.w800),
                ),
                subtitle: const Text('Protege el acceso a tu cuenta.'),
                trailing: const Icon(Icons.chevron_right_rounded),
                onTap: _changePassword,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class UserProfile {
  const UserProfile({
    required this.id,
    required this.names,
    required this.lastNames,
    required this.email,
    required this.roles,
    this.permissions = const [],
  });

  final int id;
  final String names;
  final String lastNames;
  final String email;
  final List<String> roles;
  final List<String> permissions;

  factory UserProfile.fromJson(Map<String, dynamic> json) => UserProfile(
    id: json['id'] as int,
    names: json['nombres'] as String,
    lastNames: json['apellidos'] as String,
    email: json['correo'] as String,
    roles: List<String>.from(json['roles'] as List),
    permissions: List<String>.from(
      (json['perfilOrganizacionActiva'] as Map<String, dynamic>?)?['permisos'] as List? ?? json['permisos'] as List? ?? const [],
    ),
  );
}

class OrganizationModel {
  const OrganizationModel({
    required this.id,
    required this.name,
    required this.slug,
    required this.status,
  });

  final int id;
  final String name;
  final String slug;
  final String status;

  factory OrganizationModel.fromJson(Map<String, dynamic> json) =>
      OrganizationModel(
        id: (json['id'] as num).toInt(),
        name: json['nombre'] as String? ?? 'Organizacion',
        slug: json['slug'] as String? ?? '',
        status: json['estado'] as String? ?? 'ACTIVA',
      );
}

class SessionTokens {
  const SessionTokens({required this.accessToken, required this.refreshToken});
  final String accessToken;
  final String refreshToken;
  Map<String, String> toJson() => {
    'accessToken': accessToken,
    'refreshToken': refreshToken,
  };
  factory SessionTokens.fromJson(Map<String, dynamic> json) => SessionTokens(
    accessToken: json['accessToken'] as String,
    refreshToken: json['refreshToken'] as String,
  );
}

class SessionController extends ChangeNotifier {
  SessionController(this._api, {FlutterSecureStorage? storage})
    : _storage = storage ?? const FlutterSecureStorage();

  final ApiClient _api;
  final FlutterSecureStorage _storage;
  final UiPrivilegeService uiPrivileges = UiPrivilegeService();
  UserProfile? user;
  SessionTokens? _tokens;
  List<OrganizationModel> organizations = const [];
  OrganizationModel? activeOrganization;
  bool organizationsLoaded = false;
  int organizationVersion = 0;
  bool get puedeRegistrarAsistencia => user?.permissions.contains('ASISTENCIA_REGISTRAR') ?? false;
  bool get puedeConsultarHistorialParticipacion => user?.roles.contains('PARTICIPANTE') ?? false;

  Future<AttendanceModel> registrarAsistencia(int idInscripcion) => _authorized(
    (tokens, organizationId) => _api.registrarAsistencia(tokens, idInscripcion, organizationId: organizationId),
  );

  Future<void> restore() async {
    final raw = await _storage.read(key: 'sesion');
    if (raw == null) return;
    try {
      _tokens = SessionTokens.fromJson(jsonDecode(raw) as Map<String, dynamic>);
      await _restoreOrganizations();
      user = await _api.currentUser(
        _tokens!,
        organizationId: activeOrganization?.id,
      );
      await _loadUiPrivileges();
      notifyListeners();
    } catch (_) {
      await _clear();
    }
  }

  Future<void> login(String email, String password) async {
    try {
      _tokens = await _api.login(email, password);
      await _persist();
      await _restoreOrganizations();
      user = await _api.currentUser(
        _tokens!,
        organizationId: activeOrganization?.id,
      );
      await _loadUiPrivileges();
      notifyListeners();
    } catch (_) {
      await _clear();
      rethrow;
    }
  }

  Future<void> _restoreOrganizations() async {
    organizations = await _authorizedOrganizations();
    organizationsLoaded = true;
    final saved = int.tryParse(
      await _storage.read(key: 'organizacion-activa') ?? '',
    );
    final matches = organizations.where(
      (organization) => organization.id == saved,
    );
    activeOrganization = matches.isEmpty
        ? (organizations.isEmpty ? null : organizations.first)
        : matches.first;
    if (activeOrganization == null) {
      await _storage.delete(key: 'organizacion-activa');
    } else {
      await _storage.write(
        key: 'organizacion-activa',
        value: '${activeOrganization!.id}',
      );
    }
    organizationVersion++;
  }

  Future<List<OrganizationModel>> _authorizedOrganizations() async {
    if (_tokens == null) {
      throw const ApiException('La sesion ya no esta disponible');
    }
    try {
      return await _api.listOrganizations(_tokens!);
    } on ApiException catch (error) {
      if (error.status != 401) rethrow;
      _tokens = await _api.refresh(_tokens!.refreshToken);
      await _persist();
      return _api.listOrganizations(_tokens!);
    }
  }

  Future<void> selectOrganization(int id) async {
    final matches = organizations.where(
      (organization) => organization.id == id,
    );
    if (matches.isEmpty || activeOrganization?.id == id) return;
    uiPrivileges.clear();
    activeOrganization = matches.first;
    await _storage.write(key: 'organizacion-activa', value: '$id');
    organizationVersion++;
    notifyListeners();
    user = await _authorized(
      (tokens, organizationId) => _api.currentUser(
        tokens,
        organizationId: organizationId,
      ),
    );
    notifyListeners();
    await _loadUiPrivileges();
    notifyListeners();
  }

  Future<void> register(
    String names,
    String lastNames,
    String email,
    String password,
  ) => _api.register(names, lastNames, email, password);

  Future<void> requestPasswordRecovery(String email) =>
      _api.requestPasswordRecovery(email);

  Future<void> resetPassword(String token, String password) =>
      _api.resetPassword(token, password);

  Future<void> updateProfile(
    String names,
    String lastNames,
    String email,
  ) async {
    user = await _authorized(
      (tokens, organizationId) => _api.updateProfile(
        tokens,
        names,
        lastNames,
        email,
        organizationId: organizationId,
      ),
    );
    notifyListeners();
  }

  Future<void> changePassword(String current, String next) => _authorized(
    (tokens, organizationId) => _api.changePassword(
      tokens,
      current,
      next,
      organizationId: organizationId,
    ),
  );

  Future<List<EventModel>> listEvents(String term, String category) =>
      _authorized(
        (tokens, organizationId) => _api.listEvents(
          tokens,
          term,
          category,
          organizationId: organizationId,
        ),
      );

  Future<InscripcionModel> inscribirAEvento(int idEvento) => _authorized(
    (tokens, organizationId) =>
        _api.inscribirAEvento(tokens, idEvento, organizationId: organizationId),
  );

  Future<List<InscripcionModel>> listarMisInscripciones() => _authorized(
    (tokens, organizationId) =>
        _api.listarMisInscripciones(tokens, organizationId: organizationId),
  );

  Future<CredentialModel> obtenerCredencial(int idInscripcion) async {
    final organizationId = activeOrganization?.id;
    final credential = await _authorized(
      (tokens, organizationId) => _api.obtenerCredencial(
        tokens,
        idInscripcion,
        organizationId: organizationId,
      ),
    );
    if (organizationId != null) {
      await _storage.write(
        key: 'wallet-credencial-$organizationId-$idInscripcion',
        value: jsonEncode(credential.toJson()),
      );
    }
    return credential;
  }

  Future<CredentialModel?> credencialOffline(int idInscripcion) async {
    final organizationId = activeOrganization?.id;
    if (organizationId == null) return null;
    final raw = await _storage.read(
      key: 'wallet-credencial-$organizationId-$idInscripcion',
    );
    if (raw == null) return null;
    try {
      return CredentialModel.fromJson(jsonDecode(raw) as Map<String, dynamic>);
    } catch (_) {
      return null;
    }
  }

  Future<List<HistorialParticipacionModel>> listarMiHistorial() => _authorized(
    (tokens, organizationId) =>
        _api.listarMiHistorial(tokens, organizationId: organizationId),
  );

  Future<InscripcionModel> cancelarInscripcion(int idInscripcion) =>
      _authorized(
        (tokens, organizationId) => _api.cancelarInscripcion(
          tokens,
          idInscripcion,
          organizationId: organizationId,
        ),
      );

  Future<DisponibilidadModel> obtenerDisponibilidad(int idEvento) =>
      _authorized(
        (tokens, organizationId) => _api.obtenerDisponibilidad(
          tokens,
          idEvento,
          organizationId: organizationId,
        ),
      );

  Future<List<PostulacionStaffModel>> listarMisPostulaciones() => _authorized(
    (tokens, organizationId) =>
        _api.listarMisPostulaciones(tokens, organizationId: organizationId),
  );

  Future<PostulacionStaffModel> crearPostulacionStaff(
    int idConvocatoria,
    String motivacion,
  ) => _authorized(
    (tokens, organizationId) => _api.crearPostulacionStaff(
      tokens,
      idConvocatoria,
      motivacion,
      organizationId: organizationId,
    ),
  );

  Future<List<AsignacionStaffModel>> listarMisAsignaciones() => _authorized(
    (tokens, organizationId) =>
        _api.listarMisAsignaciones(tokens, organizationId: organizationId),
  );

  Future<T> _authorized<T>(
    Future<T> Function(SessionTokens, int) action,
  ) async {
    if (_tokens == null) {
      throw const ApiException('La sesion ya no esta disponible');
    }
    if (activeOrganization == null) {
      throw const ApiException('Selecciona una organizacion para continuar');
    }
    try {
      return await action(_tokens!, activeOrganization!.id);
    } on ApiException catch (error) {
      if (error.status != 401) rethrow;
      try {
        _tokens = await _api.refresh(_tokens!.refreshToken);
        await _persist();
        return await action(_tokens!, activeOrganization!.id);
      } catch (_) {
        await _clear();
        rethrow;
      }
    }
  }

  Future<void> logout() async {
    await _clear();
    notifyListeners();
  }

  Future<void> _loadUiPrivileges() async {
    if (_tokens == null || activeOrganization == null) {
      uiPrivileges.clear();
      return;
    }
    await uiPrivileges.load(
      () => _authorized(
        (tokens, organizationId) => _api.uiPrivileges(
          tokens,
          organizationId: organizationId,
        ),
      ),
    );
  }

  Future<void> _persist() =>
      _storage.write(key: 'sesion', value: jsonEncode(_tokens!.toJson()));
  Future<void> _clear() async {
    _tokens = null;
    user = null;
    organizations = const [];
    activeOrganization = null;
    organizationsLoaded = false;
    uiPrivileges.clear();
    organizationVersion++;
    await _storage.delete(key: 'sesion');
    await _storage.delete(key: 'organizacion-activa');
  }
}

class ApiClient {
  ApiClient(this.baseUrl);
  final String baseUrl;

  Future<AttendanceModel> registrarAsistencia(SessionTokens tokens, int idInscripcion, {int? organizationId}) async {
    final response = await _send('POST', '/asistencias/check-in', tokens, body: {'idInscripcion': idInscripcion}, organizationId: organizationId);
    return AttendanceModel.fromJson(_data(response) as Map<String, dynamic>);
  }

  Future<SessionTokens> login(String email, String password) async {
    final response = await http.post(
      Uri.parse('$baseUrl/autenticacion/iniciar-sesion'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'correo': email, 'clave': password}),
    );
    return SessionTokens.fromJson(_data(response));
  }

  Future<SessionTokens> refresh(String token) async {
    final response = await http.post(
      Uri.parse('$baseUrl/autenticacion/refrescar'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'tokenRefresco': token}),
    );
    return SessionTokens.fromJson(_data(response));
  }

  Future<void> register(
    String names,
    String lastNames,
    String email,
    String password,
  ) async {
    _data(
      await _publicSend('POST', '/autenticacion/registro', {
        'nombres': names,
        'apellidos': lastNames,
        'correo': email,
        'clave': password,
      }),
    );
  }

  Future<void> requestPasswordRecovery(String email) async {
    _data(
      await _publicSend('POST', '/autenticacion/solicitar-recuperacion-clave', {
        'correo': email,
      }),
    );
  }

  Future<void> resetPassword(String token, String password) async {
    _data(
      await _publicSend('POST', '/autenticacion/restablecer-clave', {
        'token': token,
        'nuevaClave': password,
      }),
    );
  }

  Future<UserProfile> currentUser(
    SessionTokens tokens, {
    int? organizationId,
  }) async => UserProfile.fromJson(
    _data(
      await _get(
        '/autenticacion/usuario-actual',
        tokens,
        organizationId: organizationId,
      ),
    ),
  );

  Future<List<OrganizationModel>> listOrganizations(
    SessionTokens tokens,
  ) async {
    final data = _data(await _get('/organizaciones/mias', tokens));
    return (data as List<dynamic>)
        .map((item) => OrganizationModel.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<Map<String, dynamic>> uiPrivileges(
    SessionTokens tokens, {
    required int organizationId,
  }) async => _data(
    await _get(
      '/componentes-ui/actual/privilegios',
      tokens,
      organizationId: organizationId,
    ),
  ) as Map<String, dynamic>;

  Future<UserProfile> updateProfile(
    SessionTokens tokens,
    String names,
    String lastNames,
    String email, {
    int? organizationId,
  }) async => UserProfile.fromJson(
    _data(
      await _send(
        'PUT',
        '/perfil',
        tokens,
        body: {'nombres': names, 'apellidos': lastNames, 'correo': email},
        organizationId: organizationId,
      ),
    ),
  );
  Future<void> changePassword(
    SessionTokens tokens,
    String current,
    String next, {
    int? organizationId,
  }) async {
    _data(
      await _send(
        'PATCH',
        '/perfil/clave',
        tokens,
        body: {'claveActual': current, 'nuevaClave': next},
        organizationId: organizationId,
      ),
    );
  }

  Future<List<EventModel>> listEvents(
    SessionTokens tokens,
    String term,
    String category, {
    int? organizationId,
  }) async {
    final query = <String, String>{};
    if (term.trim().isNotEmpty) query['termino'] = term.trim();
    if (category.isNotEmpty) query['categoria'] = category;
    final uri = Uri.parse('$baseUrl/eventos').replace(queryParameters: query);
    final response = await http.get(
      uri,
      headers: _headers(tokens, organizationId: organizationId),
    );
    final data = _data(response);
    return (data as List<dynamic>)
        .map((item) => EventModel.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<InscripcionModel> inscribirAEvento(
    SessionTokens tokens,
    int idEvento, {
    int? organizationId,
  }) async {
    final response = await _send(
      'POST',
      '/inscripciones/eventos/$idEvento',
      tokens,
      organizationId: organizationId,
    );
    return InscripcionModel.fromJson(_data(response) as Map<String, dynamic>);
  }

  Future<List<InscripcionModel>> listarMisInscripciones(
    SessionTokens tokens, {
    int? organizationId,
  }) async {
    final response = await _get(
      '/inscripciones/mis-inscripciones',
      tokens,
      organizationId: organizationId,
    );
    final data = _data(response);
    return (data as List<dynamic>)
        .map((item) => InscripcionModel.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<CredentialModel> obtenerCredencial(
    SessionTokens tokens,
    int idInscripcion, {
    int? organizationId,
  }) async {
    final response = await _get(
      '/inscripciones/$idInscripcion/credencial',
      tokens,
      organizationId: organizationId,
    );
    return CredentialModel.fromJson(
      _data(response) as Map<String, dynamic>,
    );
  }

  Future<List<HistorialParticipacionModel>> listarMiHistorial(
    SessionTokens tokens, {
    int? organizationId,
  }) async {
    final response = await _get(
      '/inscripciones/mi-historial',
      tokens,
      organizationId: organizationId,
    );
    final data = _data(response);
    return (data as List<dynamic>)
        .map((item) => HistorialParticipacionModel.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<InscripcionModel> cancelarInscripcion(
    SessionTokens tokens,
    int idInscripcion, {
    int? organizationId,
  }) async {
    final response = await _send(
      'PATCH',
      '/inscripciones/$idInscripcion/cancelar',
      tokens,
      organizationId: organizationId,
    );
    return InscripcionModel.fromJson(_data(response) as Map<String, dynamic>);
  }

  Future<DisponibilidadModel> obtenerDisponibilidad(
    SessionTokens tokens,
    int idEvento, {
    int? organizationId,
  }) async {
    final response = await _get(
      '/inscripciones/eventos/$idEvento/disponibilidad',
      tokens,
      organizationId: organizationId,
    );
    return DisponibilidadModel.fromJson(
      _data(response) as Map<String, dynamic>,
    );
  }

  Future<List<PostulacionStaffModel>> listarMisPostulaciones(
    SessionTokens tokens, {
    int? organizationId,
  }) async {
    final data = _data(
      await _get(
        '/postulaciones-staff/mias',
        tokens,
        organizationId: organizationId,
      ),
    );
    return (data as List<dynamic>)
        .map(
          (item) =>
              PostulacionStaffModel.fromJson(item as Map<String, dynamic>),
        )
        .toList();
  }

  Future<PostulacionStaffModel> crearPostulacionStaff(
    SessionTokens tokens,
    int idConvocatoria,
    String motivacion, {
    int? organizationId,
  }) async {
    final response = await _send(
      'POST',
      '/postulaciones-staff/convocatorias/$idConvocatoria',
      tokens,
      body: {'motivacion': motivacion},
      organizationId: organizationId,
    );
    return PostulacionStaffModel.fromJson(
      _data(response) as Map<String, dynamic>,
    );
  }

  Future<List<AsignacionStaffModel>> listarMisAsignaciones(
    SessionTokens tokens, {
    int? organizationId,
  }) async {
    final data = _data(
      await _get(
        '/asignaciones-staff/mias',
        tokens,
        organizationId: organizationId,
      ),
    );
    return (data as List<dynamic>)
        .map(
          (item) => AsignacionStaffModel.fromJson(item as Map<String, dynamic>),
        )
        .toList();
  }

  Future<http.Response> _get(
    String path,
    SessionTokens tokens, {
    int? organizationId,
  }) => http.get(
    Uri.parse('$baseUrl$path'),
    headers: _headers(tokens, organizationId: organizationId),
  );
  Future<http.Response> _send(
    String method,
    String path,
    SessionTokens tokens, {
    Map<String, dynamic> body = const {},
    int? organizationId,
  }) {
    final request = http.Request(method, Uri.parse('$baseUrl$path'))
      ..headers.addAll(_headers(tokens, organizationId: organizationId));
    if (body.isNotEmpty ||
        method == 'POST' ||
        method == 'PUT' ||
        method == 'PATCH') {
      request.body = jsonEncode(body);
    }
    return http.Client().send(request).then(http.Response.fromStream);
  }

  Future<http.Response> _publicSend(
    String method,
    String path,
    Map<String, String> body,
  ) {
    final request = http.Request(method, Uri.parse('$baseUrl$path'))
      ..headers['Content-Type'] = 'application/json'
      ..body = jsonEncode(body);
    return http.Client().send(request).then(http.Response.fromStream);
  }

  Map<String, String> _headers(SessionTokens tokens, {int? organizationId}) => {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ${tokens.accessToken}',
    if (organizationId != null) 'X-Organization-Id': '$organizationId',
  };
  dynamic _data(http.Response response) {
    final data = response.body.isEmpty
        ? <String, dynamic>{}
        : jsonDecode(response.body);
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw ApiException(
        data['message'] as String? ?? 'La solicitud no pudo completarse',
        response.statusCode,
      );
    }
    return data;
  }
}

class EventModel {
  const EventModel({
    required this.id,
    required this.name,
    required this.description,
    required this.category,
    required this.start,
    required this.end,
    required this.place,
    required this.status,
  });

  final int id;
  final String name;
  final String description;
  final String category;
  final DateTime start;
  final DateTime? end;
  final String place;
  final String status;

  factory EventModel.fromJson(Map<String, dynamic> json) => EventModel(
    id: json['id'] as int,
    name: json['nombre'] as String,
    description: json['descripcion'] as String? ?? '',
    category: _readCategoryName(json),
    start: DateTime.parse(json['fechaInicio'] as String),
    end: json['fechaFin'] == null
        ? null
        : DateTime.parse(json['fechaFin'] as String),
    place: json['lugarPrincipal'] as String? ?? '',
    status: json['estado'] as String,
  );

  static String _readCategoryName(Map<String, dynamic> json) {
    final category = json['categoria'];
    if (category is String && category.isNotEmpty) return category;
    if (category is Map<String, dynamic>) {
      final name = category['nombre'];
      if (name is String && name.isNotEmpty) return name;
    }
    final categoryName = json['categoriaNombre'];
    return categoryName is String && categoryName.isNotEmpty
        ? categoryName
        : 'GENERAL';
  }
}

class CredentialModel {
  const CredentialModel({
    required this.idInscripcion,
    required this.nombreParticipante,
    required this.nombreEvento,
    required this.categoria,
    required this.fechaEvento,
    required this.lugar,
    required this.token,
    required this.qrPngBase64,
    required this.expiraEn,
  });

  final int idInscripcion;
  final String nombreParticipante;
  final String nombreEvento;
  final String categoria;
  final DateTime fechaEvento;
  final String lugar;
  final String token;
  final String qrPngBase64;
  final DateTime expiraEn;

  Map<String, dynamic> toJson() => {
    'idInscripcion': idInscripcion,
    'nombreParticipante': nombreParticipante,
    'nombreEvento': nombreEvento,
    'categoria': categoria,
    'fechaEvento': fechaEvento.toIso8601String(),
    'lugar': lugar,
    'token': token,
    'qrPngBase64': qrPngBase64,
    'expiraEn': expiraEn.toIso8601String(),
  };

  factory CredentialModel.fromJson(Map<String, dynamic> json) => CredentialModel(
    idInscripcion: json['idInscripcion'] as int,
    nombreParticipante: json['nombreParticipante'] as String,
    nombreEvento: json['nombreEvento'] as String,
    categoria: json['categoria'] as String,
    fechaEvento: DateTime.parse(json['fechaEvento'] as String),
    lugar: json['lugar'] as String,
    token: json['token'] as String,
    qrPngBase64: json['qrPngBase64'] as String,
    expiraEn: DateTime.parse(json['expiraEn'] as String),
  );
}

class InscripcionModel {
  const InscripcionModel({
    required this.idInscripcion,
    required this.idEvento,
    required this.nombreEvento,
    required this.descripcionEvento,
    required this.categoriaEvento,
    required this.fechaInicioEvento,
    required this.fechaFinEvento,
    required this.lugarEvento,
    required this.estado,
    required this.codigoQr,
    required this.fechaInscripcion,
  });

  final int idInscripcion;
  final int idEvento;
  final String nombreEvento;
  final String descripcionEvento;
  final String categoriaEvento;
  final DateTime fechaInicioEvento;
  final DateTime? fechaFinEvento;
  final String lugarEvento;
  final String estado;
  final String codigoQr;
  final DateTime fechaInscripcion;

  factory InscripcionModel.fromJson(Map<String, dynamic> json) =>
      InscripcionModel(
        idInscripcion: json['idInscripcion'] as int,
        idEvento: json['idEvento'] as int,
        nombreEvento: json['nombreEvento'] as String? ?? 'Evento',
        descripcionEvento: json['descripcionEvento'] as String? ?? '',
        categoriaEvento: json['categoriaEvento'] as String? ?? 'GENERAL',
        fechaInicioEvento: DateTime.parse(json['fechaInicioEvento'] as String),
        fechaFinEvento: json['fechaFinEvento'] != null
            ? DateTime.parse(json['fechaFinEvento'] as String)
            : null,
        lugarEvento: json['lugarEvento'] as String? ?? '',
        estado: json['estado'] as String? ?? 'CONFIRMADA',
        codigoQr: json['codigoQr'] as String? ?? '',
        fechaInscripcion: DateTime.parse(json['fechaInscripcion'] as String),
      );
}

class HistorialParticipacionModel {
  const HistorialParticipacionModel({
    required this.idInscripcion,
    required this.nombreEvento,
    required this.categoriaEvento,
    required this.fechaInicioEvento,
    required this.lugarEvento,
    required this.fechaInscripcion,
    required this.estadoInscripcion,
    required this.asistio,
    required this.fechaAsistencia,
  });

  final int idInscripcion;
  final String nombreEvento;
  final String categoriaEvento;
  final DateTime fechaInicioEvento;
  final String lugarEvento;
  final DateTime fechaInscripcion;
  final String estadoInscripcion;
  final bool asistio;
  final DateTime? fechaAsistencia;

  factory HistorialParticipacionModel.fromJson(Map<String, dynamic> json) =>
      HistorialParticipacionModel(
        idInscripcion: (json['idInscripcion'] as num).toInt(),
        nombreEvento: json['nombreEvento'] as String? ?? 'Evento',
        categoriaEvento: json['categoriaEvento'] as String? ?? 'GENERAL',
        fechaInicioEvento: DateTime.parse(json['fechaInicioEvento'] as String),
        lugarEvento: json['lugarEvento'] as String? ?? 'Sin lugar asignado',
        fechaInscripcion: DateTime.parse(json['fechaInscripcion'] as String),
        estadoInscripcion: json['estadoInscripcion'] as String? ?? 'CONFIRMADA',
        asistio: json['asistio'] as bool? ?? false,
        fechaAsistencia: json['fechaAsistencia'] == null
            ? null
            : DateTime.parse(json['fechaAsistencia'] as String),
      );
}

class DisponibilidadModel {
  const DisponibilidadModel({
    required this.idEvento,
    required this.aforoMaximo,
    required this.cuposOcupados,
    required this.cuposDisponibles,
    required this.tieneCupo,
    required this.estaInscrito,
    required this.idInscripcionActiva,
  });

  final int idEvento;
  final int? aforoMaximo;
  final int cuposOcupados;
  final int? cuposDisponibles;
  final bool tieneCupo;
  final bool estaInscrito;
  final int? idInscripcionActiva;

  factory DisponibilidadModel.fromJson(Map<String, dynamic> json) =>
      DisponibilidadModel(
        idEvento: json['idEvento'] as int,
        aforoMaximo: json['aforoMaximo'] as int?,
        cuposOcupados: (json['cuposOcupados'] as num).toInt(),
        cuposDisponibles: json['cuposDisponibles'] as int?,
        tieneCupo: json['tieneCupo'] as bool? ?? true,
        estaInscrito: json['estaInscrito'] as bool? ?? false,
        idInscripcionActiva: json['idInscripcionActiva'] as int?,
      );
}

class PostulacionStaffModel {
  const PostulacionStaffModel({
    required this.id,
    required this.idConvocatoria,
    required this.idUsuario,
    required this.estado,
    required this.motivacion,
    required this.fechaPostulacion,
  });

  final int id;
  final int idConvocatoria;
  final int idUsuario;
  final String estado;
  final String? motivacion;
  final DateTime? fechaPostulacion;

  factory PostulacionStaffModel.fromJson(Map<String, dynamic> json) =>
      PostulacionStaffModel(
        id: (json['id'] as num).toInt(),
        idConvocatoria: (json['idConvocatoria'] as num).toInt(),
        idUsuario: (json['idUsuario'] as num).toInt(),
        estado: json['estado'] as String? ?? 'PENDIENTE',
        motivacion: json['motivacion'] as String?,
        fechaPostulacion: json['fechaPostulacion'] == null
            ? null
            : DateTime.parse(json['fechaPostulacion'] as String),
      );
}

class AsignacionStaffModel {
  const AsignacionStaffModel({
    required this.id,
    required this.idPostulacion,
    required this.idUsuario,
    required this.tarea,
    required this.turnoInicio,
    required this.turnoFin,
    required this.estado,
  });

  final int id;
  final int idPostulacion;
  final int idUsuario;
  final String tarea;
  final DateTime? turnoInicio;
  final DateTime? turnoFin;
  final String estado;

  factory AsignacionStaffModel.fromJson(Map<String, dynamic> json) =>
      AsignacionStaffModel(
        id: (json['id'] as num).toInt(),
        idPostulacion: (json['idPostulacion'] as num).toInt(),
        idUsuario: (json['idUsuario'] as num).toInt(),
        tarea: json['tarea'] as String? ?? 'Tarea sin nombre',
        turnoInicio: json['turnoInicio'] == null
            ? null
            : DateTime.parse(json['turnoInicio'] as String),
        turnoFin: json['turnoFin'] == null
            ? null
            : DateTime.parse(json['turnoFin'] as String),
        estado: json['estado'] as String? ?? 'ASIGNADA',
      );
}

class ApiException implements Exception {
  const ApiException(this.message, [this.status]);
  final String message;
  final int? status;
}

class AttendanceModel {
  const AttendanceModel({required this.idAsistencia, required this.idInscripcion, required this.participante, required this.nombreEvento, required this.fechaHora, required this.nombreRegistradoPor});
  final int idAsistencia;
  final int idInscripcion;
  final String participante;
  final String nombreEvento;
  final DateTime fechaHora;
  final String nombreRegistradoPor;

  factory AttendanceModel.fromJson(Map<String, dynamic> json) => AttendanceModel(
    idAsistencia: (json['idAsistencia'] as num).toInt(),
    idInscripcion: (json['idInscripcion'] as num).toInt(),
    participante: json['participante'] as String? ?? '',
    nombreEvento: json['nombreEvento'] as String? ?? 'Evento',
    fechaHora: DateTime.parse(json['fechaHora'] as String),
    nombreRegistradoPor: json['nombreRegistradoPor'] as String? ?? '',
  );
}
