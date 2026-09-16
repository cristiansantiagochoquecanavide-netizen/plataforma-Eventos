import 'dart:convert';
import 'dart:async';

import 'package:app_links/app_links.dart';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;

import 'core/theme/app_theme.dart';
import 'convocatorias/pages/convocatorias_page.dart';
import 'convocatorias/models/convocatoria_model.dart';
import 'convocatorias/models/postulacion_staff_model.dart';

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
  bool _convocatoriasVisited = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _currentIndex,
        children: [
          EventsPage(
            session: widget.session,
            onGoToInscriptions: () => setState(() => _currentIndex = 1),
          ),
          MyInscriptionsPage(
            session: widget.session,
          ),
          ProfilePage(
            session: widget.session,
          ),
          if (_convocatoriasVisited)
            ConvocatoriasPage(session: widget.session)
          else
            const SizedBox.shrink(),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _currentIndex,
        onDestinationSelected: (index) => setState(() {
          _currentIndex = index;
          if (index == 3) _convocatoriasVisited = true;
        }),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.event_outlined),
            selectedIcon: Icon(Icons.event_rounded),
            label: 'Eventos',
          ),
          NavigationDestination(
            icon: Icon(Icons.confirmation_number_outlined),
            selectedIcon: Icon(Icons.confirmation_number_rounded),
            label: 'Inscripciones',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline_rounded),
            selectedIcon: Icon(Icons.person_rounded),
            label: 'Mi Perfil',
          ),
          NavigationDestination(
            icon: Icon(Icons.groups_outlined),
            selectedIcon: Icon(Icons.groups_rounded),
            label: 'Convocatorias',
          ),
        ],
      ),
    );
  }
}

// Pantalla del CU7: consulta eventos, busca por nombre, filtra por categoria y abre el detalle (CU11).
class EventsPage extends StatefulWidget {
  const EventsPage({
    required this.session,
    this.onGoToInscriptions,
    super.key,
  });

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

  @override
  void initState() {
    super.initState();
    _loading = false;
  }

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
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
        _categories = events.map((event) => event.category).toSet().toList()..sort();
        _loading = false;
      });
    } on ApiException catch (error) {
      if (mounted) setState(() { _error = error.message; _events = []; _loading = false; });
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

  String _formatDate(DateTime date) => '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year} ${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Eventos disponibles'),
        actions: [
          IconButton(
            onPressed: () {
              if (widget.onGoToInscriptions != null) {
                widget.onGoToInscriptions!();
              } else {
                Navigator.of(context).push(
                  MaterialPageRoute(builder: (_) => MyInscriptionsPage(session: widget.session)),
                );
              }
            },
            icon: const Icon(Icons.confirmation_number_outlined),
            tooltip: 'Mis inscripciones',
          ),
          IconButton(onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => ProfilePage(session: widget.session))), icon: const Icon(Icons.person_outline_rounded), tooltip: 'Mi perfil'),
          IconButton(onPressed: () => widget.session.logout(), icon: const Icon(Icons.logout_rounded), tooltip: 'Cerrar sesion'),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadEvents,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 30),
          children: [
            const Text('Consulta y busqueda de eventos', style: TextStyle(color: AppColors.muted)),
            const SizedBox(height: 16),
            TextField(controller: _search, onSubmitted: (_) => _loadEvents(), decoration: InputDecoration(labelText: 'Buscar por titulo o nombre', prefixIcon: const Icon(Icons.search_rounded), suffixIcon: IconButton(onPressed: _loadEvents, icon: const Icon(Icons.arrow_forward_rounded)))),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(value: _category.isEmpty ? null : _category, decoration: const InputDecoration(labelText: 'Filtrar por categoria', prefixIcon: Icon(Icons.category_outlined)), items: [const DropdownMenuItem(value: '', child: Text('Todas las categorias')), ..._categories.map((category) => DropdownMenuItem(value: category, child: Text(category)))], onChanged: (value) { setState(() => _category = value ?? ''); _loadEvents(); }),
            Align(alignment: Alignment.centerRight, child: TextButton(onPressed: _clearFilters, child: const Text('Limpiar filtros'))),
            if (_error != null) Text(_error!, style: const TextStyle(color: AppColors.danger)),
            if (_loading) const Padding(padding: EdgeInsets.all(30), child: Center(child: CircularProgressIndicator())),
            if (!_loading && _searched && _events.isEmpty) const Padding(padding: EdgeInsets.all(30), child: Center(child: Text('No se encontraron eventos con los criterios seleccionados.', textAlign: TextAlign.center))),
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
),          ],
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
      if (mounted) setState(() { _disponibilidad = disp; _loading = false; });
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
          SnackBar(
            content: Text(e.message),
            backgroundColor: AppColors.danger,
          ),
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
                    style: Theme.of(context)
                        .textTheme
                        .headlineSmall
                        ?.copyWith(fontWeight: FontWeight.w800),
                  ),
                ),
                const SizedBox(width: 8),
                Chip(label: Text(event.category)),
              ],
            ),
            const SizedBox(height: 8),
            Text(event.description.isEmpty ? 'Sin descripción' : event.description),
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
              title: Text(event.place.isEmpty ? 'Sin lugar asignado' : event.place),
              subtitle: const Text('Lugar'),
            ),
            const Divider(),
            const SizedBox(height: 4),
            // Sección CU11: Aforo y Control de Cupos
            if (_loading)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 12),
                child: Center(child: SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))),
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
                        const Icon(Icons.people_alt_outlined, color: AppColors.forest),
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
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: _disponibilidad!.tieneCupo ? Colors.green.shade100 : Colors.red.shade100,
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Text(
                          '${_disponibilidad!.cuposDisponibles} disp.',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: _disponibilidad!.tieneCupo ? Colors.green.shade900 : Colors.red.shade900,
                          ),
                        ),
                      ),
                  ],
                ),
              ),
              if (_error != null)
                Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: Text(_error!, style: const TextStyle(color: AppColors.danger, fontSize: 13)),
                ),
              const SizedBox(height: 12),
              if (_disponibilidad!.estaInscrito)
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
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
                        style: TextStyle(color: Colors.green, fontWeight: FontWeight.bold),
                      ),
                    ],
                  ),
                )
              else
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    onPressed: (!_disponibilidad!.tieneCupo || _inscribiendo) ? null : _inscribir,
                    icon: _inscribiendo
                        ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                        : const Icon(Icons.how_to_reg_rounded),
                    label: Text(_inscribiendo
                        ? 'Inscribiendo...'
                        : (_disponibilidad!.tieneCupo ? 'Inscribirme al evento' : 'Cupos agotados')),
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

  @override
  void initState() {
    super.initState();
    _loadInscriptions();
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
        SnackBar(
          content: Text(e.message),
          backgroundColor: AppColors.danger,
        ),
      );
    }
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
      body: RefreshIndicator(
        onRefresh: _loadInscriptions,
        child: _buildBody(),
      ),
    );
  }

  Widget _buildBody() {
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
                const Icon(Icons.error_outline_rounded, size: 48, color: AppColors.danger),
                const SizedBox(height: 12),
                Text(_error!, textAlign: TextAlign.center, style: const TextStyle(color: AppColors.danger)),
                const SizedBox(height: 16),
                FilledButton(onPressed: _loadInscriptions, child: const Text('Reintentar')),
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
                Icon(Icons.confirmation_number_outlined, size: 64, color: AppColors.muted.withOpacity(0.5)),
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

    final activasCount = _inscriptions.where((i) => i.estado == 'CONFIRMADA').length;
    final canceladasCount = _inscriptions.where((i) => i.estado == 'CANCELADA').length;

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
              onSelectionChanged: (set) => setState(() => _selectedFilter = set.first),
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
                            color: AppColors.muted.withOpacity(0.5),
                          ),
                          const SizedBox(height: 12),
                          Text(
                            _selectedFilter == 'CONFIRMADA'
                                ? 'No tienes entradas activas'
                                : 'No hay inscripciones canceladas',
                            style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                          ),
                          const SizedBox(height: 6),
                          Text(
                            _selectedFilter == 'CONFIRMADA'
                                ? 'Inscríbete a un evento en la pestaña "Eventos" para ver tu entrada y QR aquí.'
                                : 'Cuando canceles una inscripción, quedará registrada en esta sección.',
                            textAlign: TextAlign.center,
                            style: const TextStyle(color: AppColors.muted, fontSize: 13),
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
                                      color: isConfirmada ? null : Colors.grey.shade700,
                                    ),
                                  ),
                                ),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                                  decoration: BoxDecoration(
                                    color: isConfirmada ? Colors.green.shade50 : Colors.grey.shade200,
                                    border: Border.all(
                                      color: isConfirmada ? Colors.green.shade400 : Colors.grey.shade400,
                                    ),
                                    borderRadius: BorderRadius.circular(20),
                                  ),
                                  child: Row(
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      Icon(
                                        isConfirmada ? Icons.check_circle_outline : Icons.cancel_outlined,
                                        size: 14,
                                        color: isConfirmada ? Colors.green.shade800 : Colors.grey.shade700,
                                      ),
                                      const SizedBox(width: 4),
                                      Text(
                                        ins.estado,
                                        style: TextStyle(
                                          fontSize: 12,
                                          fontWeight: FontWeight.bold,
                                          color: isConfirmada ? Colors.green.shade800 : Colors.grey.shade700,
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
                                    const Icon(Icons.calendar_today_rounded, size: 14, color: AppColors.muted),
                                    const SizedBox(width: 4),
                                    Text(_formatDate(ins.fechaInicioEvento), style: const TextStyle(color: AppColors.muted, fontSize: 13)),
                                  ],
                                ),
                              ],
                            ),
                            const SizedBox(height: 6),
                            Row(
                              children: [
                                const Icon(Icons.place_outlined, size: 16, color: AppColors.muted),
                                const SizedBox(width: 4),
                                Expanded(
                                  child: Text(
                                    ins.lugarEvento,
                                    style: const TextStyle(color: AppColors.muted, fontSize: 13),
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 12),
                            // Código QR / Hash (CU12)
                            Container(
                              width: double.infinity,
                              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                              decoration: BoxDecoration(
                                color: isConfirmada ? AppColors.canvas : Colors.grey.shade200,
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Row(
                                children: [
                                  Icon(
                                    isConfirmada ? Icons.qr_code_2_rounded : Icons.qr_code_scanner_rounded,
                                    size: 20,
                                    color: isConfirmada ? AppColors.forest : Colors.grey.shade600,
                                  ),
                                  const SizedBox(width: 8),
                                  Expanded(
                                    child: Text(
                                      ins.codigoQr,
                                      style: TextStyle(
                                        fontFamily: 'monospace',
                                        fontWeight: FontWeight.w600,
                                        fontSize: 13,
                                        color: isConfirmada ? null : Colors.grey.shade600,
                                        decoration: isConfirmada ? null : TextDecoration.lineThrough,
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
                              style: const TextStyle(color: AppColors.muted, fontSize: 11),
                            ),
                            if (isConfirmada) ...[
                              const SizedBox(height: 12),
                              Align(
                                alignment: Alignment.centerRight,
                                child: OutlinedButton.icon(
                                  style: OutlinedButton.styleFrom(
                                    foregroundColor: AppColors.danger,
                                    side: const BorderSide(color: AppColors.danger),
                                  ),
                                  onPressed: () => _cancelarInscripcion(ins),
                                  icon: const Icon(Icons.cancel_outlined, size: 16),
                                  label: const Text('Cancelar inscripción'),
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
            Card(
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
                      decoration: const InputDecoration(
                        labelText: 'Nombres',
                        prefixIcon: Icon(Icons.person_outline_rounded),
                      ),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: _lastNames,
                      decoration: const InputDecoration(
                        labelText: 'Apellidos',
                        prefixIcon: Icon(Icons.badge_outlined),
                      ),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: _email,
                      keyboardType: TextInputType.emailAddress,
                      decoration: const InputDecoration(
                        labelText: 'Correo',
                        prefixIcon: Icon(Icons.alternate_email_rounded),
                      ),
                    ),
                    const SizedBox(height: 18),
                    FilledButton.icon(
                      onPressed: _saving ? null : _save,
                      icon: const Icon(Icons.save_rounded),
                      label: Text(_saving ? 'Guardando...' : 'Guardar cambios'),
                    ),
                  ],
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
  });

  final int id;
  final String names;
  final String lastNames;
  final String email;
  final List<String> roles;

  factory UserProfile.fromJson(Map<String, dynamic> json) => UserProfile(
    id: json['id'] as int,
    names: json['nombres'] as String,
    lastNames: json['apellidos'] as String,
    email: json['correo'] as String,
    roles: List<String>.from(json['roles'] as List),
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
  SessionController(this._api);

  final ApiClient _api;
  final FlutterSecureStorage _storage = const FlutterSecureStorage();
  UserProfile? user;
  SessionTokens? _tokens;
  Future<SessionTokens>? _refreshing;

  Future<List<ConvocatoriaModel>> listarConvocatorias() =>
      _authorized(_api.listarConvocatorias);

  Future<ConvocatoriaModel> obtenerConvocatoria(int id) =>
      _authorized((tokens) => _api.obtenerConvocatoria(tokens, id));

  Future<PostulacionStaffModel> postularse(int id) =>
      _authorized((tokens) => _api.postularse(tokens, id));

  Future<PostulacionStaffModel?> consultarPostulacion(int id) =>
      _authorized((tokens) => _api.consultarPostulacion(tokens, id));

  Future<List<PostulacionStaffModel>> listarMisPostulaciones() =>
      _authorized(_api.listarMisPostulaciones);

  Future<PostulacionStaffModel> retirarPostulacion(int id) =>
      _authorized((tokens) => _api.retirarPostulacion(tokens, id));

  Future<T> _authorized<T>(Future<T> Function(SessionTokens) action) async {
    const expired = ApiException(
      'Tu sesion vencio. Inicia sesion nuevamente.',
      401,
    );
    final original = _tokens;
    final originalUser = user;
    if (original == null) throw expired;
    try {
      return await action(original);
    } on ApiException catch (error) {
      if (error.status != 401) rethrow;
      if (!identical(user, originalUser)) throw expired;
      // Todas las peticiones comparten una renovacion: el backend rota el token.
      if (identical(_tokens, original)) {
        final pending = _refreshing ??= _renewSession(original);
        try {
          await pending;
        } finally {
          if (identical(_refreshing, pending)) _refreshing = null;
        }
      }
      final renewed = _tokens;
      if (renewed == null) throw expired;
      // Un rechazo de negocio tras renovar el JWT no invalida la sesion.
      try {
        return await action(renewed);
      } on ApiException catch (retryError) {
        if (retryError.status == 401 && identical(_tokens, renewed)) {
          await _clear();
          notifyListeners();
          throw expired;
        }
        rethrow;
      }
    }
  }

  Future<SessionTokens> _renewSession(SessionTokens original) async {
    try {
      final renewed = await _api
          .refresh(original.refreshToken)
          .timeout(const Duration(seconds: 20));
      // No restaurar una sesion que el usuario cerro durante la peticion.
      if (!identical(_tokens, original)) {
        throw const ApiException('La sesion ya no esta disponible', 401);
      }
      _tokens = renewed;
      await _persist();
      return renewed;
    } on ApiException catch (error) {
      if ([400, 401, 403].contains(error.status) &&
          identical(_tokens, original)) {
        await _clear();
        notifyListeners();
        throw const ApiException(
          'Tu sesion vencio. Inicia sesion nuevamente.',
          401,
        );
      }
      rethrow;
    } on TimeoutException {
      throw const ApiException(
        'El servidor tarda en responder. Intenta nuevamente.',
      );
    } on http.ClientException {
      throw const ApiException(
        'No se pudo renovar la sesion. Revisa tu conexion.',
      );
    }
  }

  Future<void> restore() async {
    final raw = await _storage.read(key: 'sesion');
    if (raw == null) return;
    try {
      _tokens = SessionTokens.fromJson(jsonDecode(raw) as Map<String, dynamic>);
      user = await _api.currentUser(_tokens!);
    } catch (_) {
      await _clear();
    }
  }

  Future<void> login(String email, String password) async {
    _tokens = await _api.login(email, password);
    await _persist();
    user = await _api.currentUser(_tokens!);
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
      (tokens) => _api.updateProfile(tokens, names, lastNames, email),
    );
    notifyListeners();
  }

  Future<void> changePassword(String current, String next) =>
      _authorized((tokens) => _api.changePassword(tokens, current, next));

  Future<List<EventModel>> listEvents(String term, String category) =>
      _authorized((tokens) => _api.listEvents(tokens, term, category));

  Future<InscripcionModel> inscribirAEvento(int idEvento) =>
      _authorized((tokens) => _api.inscribirAEvento(tokens, idEvento));

  Future<List<InscripcionModel>> listarMisInscripciones() =>
      _authorized((tokens) => _api.listarMisInscripciones(tokens));

  Future<InscripcionModel> cancelarInscripcion(int idInscripcion) =>
      _authorized((tokens) => _api.cancelarInscripcion(tokens, idInscripcion));

  Future<DisponibilidadModel> obtenerDisponibilidad(int idEvento) =>
      _authorized((tokens) => _api.obtenerDisponibilidad(tokens, idEvento));

  Future<void> logout() async {
    await _clear();
    notifyListeners();
  }

  Future<void> _persist() =>
      _storage.write(key: 'sesion', value: jsonEncode(_tokens!.toJson()));
  Future<void> _clear() async {
    _tokens = null;
    user = null;
    await _storage.delete(key: 'sesion');
  }
}

class ApiClient {
  ApiClient(this.baseUrl);
  final String baseUrl;

  Future<List<ConvocatoriaModel>> listarConvocatorias(
    SessionTokens tokens,
  ) async {
    final data = await _convocatoriasData(
      _get('/convocatorias?estado=ABIERTA', tokens),
    );
    return (data as List)
        .map((item) => ConvocatoriaModel.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<ConvocatoriaModel> obtenerConvocatoria(
    SessionTokens tokens,
    int id,
  ) async => ConvocatoriaModel.fromJson(
    await _convocatoriasData(_get('/convocatorias/$id', tokens))
        as Map<String, dynamic>,
  );

  Future<PostulacionStaffModel> postularse(
    SessionTokens tokens,
    int id,
  ) async => PostulacionStaffModel.fromJson(
    await _convocatoriasData(
          _send('POST', '/convocatorias/$id/postulaciones', tokens, {}),
        )
        as Map<String, dynamic>,
  );

  Future<PostulacionStaffModel?> consultarPostulacion(
    SessionTokens tokens,
    int id,
  ) async {
    final data = await _convocatoriasData(
      _get('/convocatorias/$id/postulacion', tokens),
    );
    return data == null
        ? null
        : PostulacionStaffModel.fromJson(data as Map<String, dynamic>);
  }

  Future<List<PostulacionStaffModel>> listarMisPostulaciones(
    SessionTokens tokens,
  ) async {
    final data = await _convocatoriasData(_get('/postulaciones/mias', tokens));
    return (data as List)
        .map(
          (item) =>
              PostulacionStaffModel.fromJson(item as Map<String, dynamic>),
        )
        .toList();
  }

  Future<PostulacionStaffModel> retirarPostulacion(
    SessionTokens tokens,
    int id,
  ) async => PostulacionStaffModel.fromJson(
    await _convocatoriasData(
          _send('PATCH', '/postulaciones/$id/retirar', tokens, {}),
        )
        as Map<String, dynamic>,
  );

  // Estas rutas devuelven objetos, listas o 204; _data conserva su contrato actual.
  Future<dynamic> _convocatoriasData(Future<http.Response> request) async {
    try {
      final response = await request.timeout(const Duration(seconds: 20));
      dynamic data;
      try {
        data = response.body.isEmpty
            ? null
            : jsonDecode(utf8.decode(response.bodyBytes));
      } on FormatException {
        if (response.statusCode >= 200 && response.statusCode < 300) {
          throw const ApiException('La respuesta del servidor no es valida.');
        }
      }
      if (response.statusCode < 200 || response.statusCode >= 300) {
        throw ApiException(
          data is Map && data['message'] is String
              ? data['message'] as String
              : 'La solicitud no pudo completarse',
          response.statusCode,
        );
      }
      return data;
    } on TimeoutException {
      throw const ApiException(
        'El servidor tarda en responder. Intenta nuevamente.',
      );
    } on http.ClientException {
      throw const ApiException(
        'No se pudo conectar. Revisa tu conexion e intenta nuevamente.',
      );
    }
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

  Future<UserProfile> currentUser(SessionTokens tokens) async =>
      UserProfile.fromJson(
        _data(await _get('/autenticacion/usuario-actual', tokens)),
      );
  Future<UserProfile> updateProfile(
    SessionTokens tokens,
    String names,
    String lastNames,
    String email,
  ) async => UserProfile.fromJson(
    _data(
      await _send('PUT', '/perfil', tokens, {
        'nombres': names,
        'apellidos': lastNames,
        'correo': email,
      }),
    ),
  );
  Future<void> changePassword(
    SessionTokens tokens,
    String current,
    String next,
  ) async {
    _data(
      await _send('PATCH', '/perfil/clave', tokens, {
        'claveActual': current,
        'nuevaClave': next,
      }),
    );
  }

  Future<List<EventModel>> listEvents(
    SessionTokens tokens,
    String term,
    String category,
  ) async {
    final query = <String, String>{};
    if (term.trim().isNotEmpty) query['termino'] = term.trim();
    if (category.isNotEmpty) query['categoria'] = category;
    final uri = Uri.parse('$baseUrl/eventos').replace(queryParameters: query);
    final response = await http.get(uri, headers: _headers(tokens));
    final data = _data(response);
    return (data as List<dynamic>)
        .map((item) => EventModel.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<InscripcionModel> inscribirAEvento(SessionTokens tokens, int idEvento) async {
    final response = await _send('POST', '/inscripciones/eventos/$idEvento', tokens);
    return InscripcionModel.fromJson(_data(response) as Map<String, dynamic>);
  }

  Future<List<InscripcionModel>> listarMisInscripciones(SessionTokens tokens) async {
    final response = await _get('/inscripciones/mis-inscripciones', tokens);
    final data = _data(response);
    return (data as List<dynamic>)
        .map((item) => InscripcionModel.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<InscripcionModel> cancelarInscripcion(SessionTokens tokens, int idInscripcion) async {
    final response = await _send('PATCH', '/inscripciones/$idInscripcion/cancelar', tokens);
    return InscripcionModel.fromJson(_data(response) as Map<String, dynamic>);
  }

  Future<DisponibilidadModel> obtenerDisponibilidad(SessionTokens tokens, int idEvento) async {
    final response = await _get('/inscripciones/eventos/$idEvento/disponibilidad', tokens);
    return DisponibilidadModel.fromJson(_data(response) as Map<String, dynamic>);
  }

  Future<http.Response> _get(String path, SessionTokens tokens) =>
      http.get(Uri.parse('$baseUrl$path'), headers: _headers(tokens));
  Future<http.Response> _send(
    String method,
    String path,
    SessionTokens tokens, [
    Map<String, dynamic> body = const {},
  ]) {
    final request = http.Request(method, Uri.parse('$baseUrl$path'))
      ..headers.addAll(_headers(tokens));
    if (body.isNotEmpty || method == 'POST' || method == 'PUT' || method == 'PATCH') {
      request.body = jsonEncode(body);
    }
    return _sendRequest(request);
  }

  Future<http.Response> _publicSend(
    String method,
    String path,
    Map<String, String> body,
  ) {
    final request = http.Request(method, Uri.parse('$baseUrl$path'))
      ..headers['Content-Type'] = 'application/json'
      ..body = jsonEncode(body);
    return _sendRequest(request);
  }

  Future<http.Response> _sendRequest(http.Request request) async {
    final client = http.Client();
    try {
      return await client
          .send(request)
          .then(http.Response.fromStream)
          .timeout(const Duration(seconds: 20));
    } finally {
      client.close();
    }
  }

  Map<String, String> _headers(SessionTokens tokens) => {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ${tokens.accessToken}',
  };
  dynamic _data(http.Response response) {
    final data = response.body.isEmpty ? <String, dynamic>{} : jsonDecode(response.body);
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
  const EventModel({required this.id, required this.name, required this.description, required this.category, required this.start, required this.end, required this.place, required this.status});

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
    end: json['fechaFin'] == null ? null : DateTime.parse(json['fechaFin'] as String),
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
    return categoryName is String && categoryName.isNotEmpty ? categoryName : 'GENERAL';
  }
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

  factory InscripcionModel.fromJson(Map<String, dynamic> json) => InscripcionModel(
    idInscripcion: json['idInscripcion'] as int,
    idEvento: json['idEvento'] as int,
    nombreEvento: json['nombreEvento'] as String? ?? 'Evento',
    descripcionEvento: json['descripcionEvento'] as String? ?? '',
    categoriaEvento: json['categoriaEvento'] as String? ?? 'GENERAL',
    fechaInicioEvento: DateTime.parse(json['fechaInicioEvento'] as String),
    fechaFinEvento: json['fechaFinEvento'] != null ? DateTime.parse(json['fechaFinEvento'] as String) : null,
    lugarEvento: json['lugarEvento'] as String? ?? '',
    estado: json['estado'] as String? ?? 'CONFIRMADA',
    codigoQr: json['codigoQr'] as String? ?? '',
    fechaInscripcion: DateTime.parse(json['fechaInscripcion'] as String),
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

  factory DisponibilidadModel.fromJson(Map<String, dynamic> json) => DisponibilidadModel(
    idEvento: json['idEvento'] as int,
    aforoMaximo: json['aforoMaximo'] as int?,
    cuposOcupados: (json['cuposOcupados'] as num).toInt(),
    cuposDisponibles: json['cuposDisponibles'] as int?,
    tieneCupo: json['tieneCupo'] as bool? ?? true,
    estaInscrito: json['estaInscrito'] as bool? ?? false,
    idInscripcionActiva: json['idInscripcionActiva'] as int?,
  );
}

class ApiException implements Exception {
  const ApiException(this.message, [this.status]);
  final String message;
  final int? status;
}

