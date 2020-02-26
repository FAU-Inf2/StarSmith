lexer grammar LaLaLexer;

@header {
 package i2.act.antlr;
}

// --[ default mode ]--

WHITESPACE: (' ' | '\t' | '\r' | '\n') -> skip;
COMMENT: '#' ~('\n'|'\r')* '\r'? '\n' -> skip;

CLASS: 'class';

LBRACE: '{';
RBRACE: '}';

LPAREN: '(';
RPAREN: ')';

LBRACK: '[';
RBRACK: ']';

BUILTIN_OPERATOR: '+'|'-'|'*'|'/'|'=='|'!='|'<'|'>'|'<='|'>=';

COMMA: ',';
COLON: ':';
SEMICOLON: ';';

DOT: '.';

DOTS: '..';

DOLLAR : '$';

EQUALS: '=';

NUMBER: '-'?[0-9]+;

QUOT_BEGIN: '"' -> pushMode(IN_STRING);

AT: '@';

INH: 'inh';
SYN: 'syn';
GRD: 'grd';
LOC: 'loc';

USE: 'use';

CHAR_LITERAL: '\'' ~('\'') '\'';

BOOLEAN_LITERAL: 'true' | 'false';

NIL: 'nil';

// NOTE: place this rule *at the bottom* so that keywords are not identified as identifier
IDENTIFIER: [a-zA-Z_]+[a-zA-Z0-9_]*;


// --[ IN_STRING mode ]--

mode IN_STRING;

LINE_BREAK: ('\r' | '\n' | '\r\n') (' ' | '\t')* -> skip;

ESCAPE_NEWLINE: '\\n';
ESCAPE_INDENT: '\\+';
ESCAPE_UNINDENT: '\\-';
ESCAPE_LPAREN: '\\(';
ESCAPE_RPAREN: '\\)';
ESCAPE_DOLLAR: '\\$';
ESCAPE_HASH: '\\#';
ESCAPE_QUOTE: '\\"';

STRING_CHAR: ~('"');
CHILD_DECLARATION_START: '${' -> pushMode(CHILD_DECLARATION);
PRINT_COMMAND_START: '#{' -> pushMode(PRINT_COMMAND);

QUOT_END: '"' -> popMode;


// --[ CHILD_DECLARATION mode ]--

mode CHILD_DECLARATION;

CHILD_DECLARATION_WHITESPACE: (' ' | '\t' | '\r' | '\n') -> skip;

CHILD_DECLARATION_IDENTIFIER: [a-zA-Z_]+[a-zA-Z0-9_]*;
CHILD_DECLARATION_COLON: ':';

CHILD_DECLARATION_STAR: '*';

CHILD_DECLARATION_END: '}' -> popMode;


// --[ PRINT_COMMAND mode ]--

mode PRINT_COMMAND;

PRINT_COMMAND_WHITESPACE: (' ' | '\t' | '\r' | '\n') -> skip;

PRINT_COMMAND_IDENTIFIER: [a-zA-Z_]+[a-zA-Z0-9_]*;
PRINT_COMMAND_DOT: '.';
PRINT_COMMAND_COLON: ':';
PRINT_COMMAND_CHAR_LITERAL: '\'' ~('\'') '\'';
PRINT_COMMAND_BOOLEAN_LITERAL: 'true' | 'false';
PRINT_COMMAND_NIL: 'nil';
PRINT_COMMAND_LPAREN: '(';
PRINT_COMMAND_RPAREN: ')';
PRINT_COMMAND_NUMBER: '-'?[0-9]+;

PRINT_COMMAND_BUILTIN_OPERATOR: '+'|'-'|'*'|'/'|'=='|'!='|'<'|'>'|'<='|'>=';

PRINT_COMMAND_END: '}' -> popMode;
