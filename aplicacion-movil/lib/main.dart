import 'dart:convert';
import 'dart:async';

import 'package:app_links/app_links.dart';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;
import 'core/theme/app_theme.dart';

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
                : EventsPage(session: widget.session);
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

// Pantalla del CU7: consulta eventos, busca por nombre, filtra por categoria y abre el detalle.
class EventsPage extends StatefulWidget {
  const EventsPage({required this.session, super.key});

  final SessionController session;

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
      builder: (context) => Padding(
        padding: const EdgeInsets.fromLTRB(22, 4, 22, 30),
        child: Wrap(
          runSpacing: 12,
          children: [
            Text(event.name, style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800)),
            Chip(label: Text(event.category)),
            Text(event.description.isEmpty ? 'Sin descripcion' : event.description),
            ListTile(leading: const Icon(Icons.calendar_today_rounded), title: Text(_formatDate(event.start)), subtitle: const Text('Fecha de inicio')),
            if (event.end != null) ListTile(leading: const Icon(Icons.event_available_rounded), title: Text(_formatDate(event.end!)), subtitle: const Text('Fecha de finalizacion')),
            ListTile(leading: const Icon(Icons.place_outlined), title: Text(event.place.isEmpty ? 'Sin lugar asignado' : event.place), subtitle: const Text('Lugar')),
            Text('Estado: ${event.status}', style: const TextStyle(fontWeight: FontWeight.w700)),
          ],
        ),
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

  Future<T> _authorized<T>(Future<T> Function(SessionTokens) action) async {
    if (_tokens == null) {
      throw const ApiException('La sesion ya no esta disponible');
    }
    try {
      return await action(_tokens!);
    } on ApiException catch (error) {
      if (error.status != 401) rethrow;
      try {
        _tokens = await _api.refresh(_tokens!.refreshToken);
        await _persist();
        return await action(_tokens!);
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

  Future<http.Response> _get(String path, SessionTokens tokens) =>
      http.get(Uri.parse('$baseUrl$path'), headers: _headers(tokens));
  Future<http.Response> _send(
    String method,
    String path,
    SessionTokens tokens,
    Map<String, String> body,
  ) {
    final request = http.Request(method, Uri.parse('$baseUrl$path'))
      ..headers.addAll(_headers(tokens))
      ..body = jsonEncode(body);
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

class ApiException implements Exception {
  const ApiException(this.message, [this.status]);
  final String message;
  final int? status;
}
