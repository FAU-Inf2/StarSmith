parser grammar LaLaParser;

@header {
 package i2.act.antlr;
}

options { tokenVocab=LaLaLexer; }

languageSpecification
  : ( useStatement )* ( classDeclaration )+ EOF
  ;

useStatement
  : USE namespace=identifier SEMICOLON
  ;

classDeclaration
  : productionClassDeclaration
  | literalClassDeclaration
  ;

productionClassDeclaration
  : ( annotation )* CLASS className=identifier LBRACE
      ( attributeDeclaration )*
      ( productionDeclaration )*
    RBRACE
  ;

literalClassDeclaration
  : ( annotation )* CLASS className=identifier LPAREN regularExpression=plainString RPAREN SEMICOLON
  ;

attributeDeclaration
  : modifier=attributeModifier attributeName=identifier ( COLON attributeTypeName )? SEMICOLON
  ;

attributeModifier
  : INH
  | SYN
  | GRD
  ;

attributeTypeName
  : name=identifier
  ;

productionDeclaration
  : treeProductionDeclaration
  | generatorProductionDeclaration
  ;

treeProductionDeclaration
  : ( annotation )* productionName=identifier LPAREN serialization RPAREN LBRACE
      ( localAttributeDefinition )*
      ( attributeEvaluationRule )*
    RBRACE
  ;

generatorProductionDeclaration
  : ( annotation )* productionName=identifier generatorCall=attributeFunctionCall COLON attributeTypeName LBRACE
      ( localAttributeDefinition )*
      ( attributeEvaluationRule )*
    RBRACE
  ;

localAttributeDefinition
  : LOC attributeName=identifier EQUALS rhs=attributeExpression SEMICOLON
  ;

childDeclaration
  : childName=identifier colon typeName
  ;

attributeEvaluationRule
  : targetAttribute=attributeAccess EQUALS rhs=attributeExpression SEMICOLON
  ;

attributeExpression
  : attributeAtom          # attributeExpressionAlternativeAtom
  | attributeFunctionCall  # attributeExpressionAlternativeFunctionCall
  | childReference         # attributeExpressionAlternativeChildReference
  ;

attributeAtom
  : attributeAccess        # attributeAtomAlternativeAttributeAccess
  | localAttributeAccess   # attributeAtomAlternativeLocalAttributeAccess
  | attributeLiteral       # attributeAtomAlternativeLiteral
  | generatorValue         # attributeAtomAlternativeGeneratorValue
  ;

childReference
  : identifier
  ;

attributeLiteral
  : number
  | plainString
  | charLiteral
  | booleanLiteral
  | nil
  ;

generatorValue
  : DOLLAR
  ;

attributeFunctionCall
  : lparen attributeFunction ( attributeExpression )* rparen
  ;

attributeAccess
  : targetName=identifier dot attributeName=identifier
  ;

localAttributeAccess
  : dot attributeName=identifier
  ;

attributeFunction
  : namespace=identifier colon functionName=identifier  # runtimeFunction
  | functionName=attributeFunctionName                  # builtinFunction
  ;

attributeFunctionName
  : identifier
  | ( BUILTIN_OPERATOR | PRINT_COMMAND_BUILTIN_OPERATOR )
  ;

annotation
  : AT annotationName=identifier
    ( LPAREN ( annotationArgument ( COMMA annotationArgument )* ( COMMA )? )? RPAREN )?
  ;

annotationArgument
  : expression
  ;

serialization
  : interpolatedString
  ;

plainString
  : QUOT_BEGIN ( plainStringElement )* QUOT_END
  ;

plainStringElement
  : ( stringCharacters | escapeSequence )
  ;

interpolatedString
  : QUOT_BEGIN ( interpolatedStringElement )* QUOT_END
  ;

interpolatedStringElement
  : ( stringCharacters | escapeSequence | stringInterpolation )
  ;

stringCharacters
  : ( STRING_CHAR )+
  ;

escapeSequence
  : ESCAPE_NEWLINE
  | ESCAPE_INDENT
  | ESCAPE_UNINDENT
  | ESCAPE_DOLLAR
  | ESCAPE_HASH
  | ESCAPE_QUOTE
  ;

stringInterpolation
  : stringInterpolationChildDeclaration
  | stringInterpolationPrintCommand
  ;

stringInterpolationChildDeclaration
  : CHILD_DECLARATION_START
      childDeclaration
    CHILD_DECLARATION_END
  | ESCAPE_LPAREN CHILD_DECLARATION_START
      childDeclaration
    CHILD_DECLARATION_END ESCAPE_RPAREN
  ;

stringInterpolationPrintCommand
  : PRINT_COMMAND_START attributeExpression PRINT_COMMAND_END
  ;

dot
  : ( DOT | PRINT_COMMAND_DOT )
  ;

colon
  : ( COLON | CHILD_DECLARATION_COLON | PRINT_COMMAND_COLON )
  ;

lparen
  : ( LPAREN | PRINT_COMMAND_LPAREN )
  ;

rparen
  : ( RPAREN | PRINT_COMMAND_RPAREN )
  ;

typeName
  : name=identifier
  ;

identifier
  : ( IDENTIFIER | CHILD_DECLARATION_IDENTIFIER | PRINT_COMMAND_IDENTIFIER )
  ;

expression
  : number          # expressionNumber
  | plainString     # expressionString
  | entityReference # expressionEntityReference
  ;

number
  : ( NUMBER | PRINT_COMMAND_NUMBER )
  ;

entityReference
  : entityName=identifier
  ;

charLiteral
  : ( CHAR_LITERAL | PRINT_COMMAND_CHAR_LITERAL )
  ;

booleanLiteral
  : ( BOOLEAN_LITERAL | PRINT_COMMAND_BOOLEAN_LITERAL )
  ;

nil
  : (NIL | PRINT_COMMAND_NIL)
  ;
