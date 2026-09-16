import 'package:flutter/material.dart';

import '../main.dart';
import 'pages/convocatorias_page.dart';

class ConvocatoriasHome extends StatefulWidget {
  const ConvocatoriasHome({required this.session, super.key});

  final SessionController session;

  @override
  State<ConvocatoriasHome> createState() => _ConvocatoriasHomeState();
}

class _ConvocatoriasHomeState extends State<ConvocatoriasHome> {
  int _selected = 0;
  bool _convocatoriasVisited = false;

  @override
  Widget build(BuildContext context) => Scaffold(
    body: IndexedStack(
      index: _selected,
      children: [
        ProfilePage(session: widget.session),
        if (_convocatoriasVisited)
          ConvocatoriasPage(session: widget.session)
        else
          const SizedBox.shrink(),
      ],
    ),
    bottomNavigationBar: NavigationBar(
      selectedIndex: _selected,
      onDestinationSelected: (index) => setState(() {
        _selected = index;
        if (index == 1) _convocatoriasVisited = true;
      }),
      destinations: const [
        NavigationDestination(
          icon: Icon(Icons.person_outline),
          label: 'Perfil',
        ),
        NavigationDestination(
          icon: Icon(Icons.groups_outlined),
          label: 'Convocatorias',
        ),
      ],
    ),
  );
}
