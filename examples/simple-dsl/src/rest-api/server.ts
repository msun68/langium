/******************************************************************************
 * Copyright 2021 TypeFox GmbH
 * This program and the accompanying materials are made available under the
 * terms of the MIT License, which is available in the project root.
 ******************************************************************************/

import express from 'express';
import { EmptyFileSystem } from 'langium';
import { parseHelper } from 'langium/test';
import { createSimpleDslServices } from '../language-server/simple-dsl-module.js';
import type { Greeting } from '../language-server/generated/ast.js';

const app = express();
const port = process.env.PORT || 3000;

// Middleware to parse JSON bodies
app.use(express.json());

// Create Langium services
const services = createSimpleDslServices(EmptyFileSystem).simpleDsl;
const parse = parseHelper<Greeting>(services);

/**
 * Parse DSL content and return AST as JSON
 */
app.post('/parse', async (req, res) => {
    try {
        const { content } = req.body;
        
        if (!content || typeof content !== 'string') {
            return res.status(400).json({ 
                error: 'Missing or invalid content field in request body' 
            });
        }

        // Parse the content
        const document = await parse(content, { validation: true });

        // Check for parse errors
        if (document.parseResult.parserErrors.length > 0) {
            return res.status(400).json({
                error: 'Parse errors',
                diagnostics: document.parseResult.parserErrors.map(e => ({
                    message: e.message
                }))
            });
        }

        // Check for validation errors
        const validationErrors = (document.diagnostics ?? []).filter(e => e.severity === 1);
        if (validationErrors.length > 0) {
            return res.status(400).json({
                error: 'Validation errors',
                diagnostics: validationErrors.map(e => ({
                    line: e.range.start.line,
                    column: e.range.start.character,
                    message: e.message,
                    severity: e.severity
                }))
            });
        }

        // Return the AST (serialize without circular references)
        const ast = document.parseResult.value;
        
        // Create a clean version without circular references
        const cleanAst = JSON.parse(JSON.stringify(ast, (key, value) => {
            // Skip internal properties that start with $ or _
            if (key.startsWith('$') || key.startsWith('_')) {
                return undefined;
            }
            return value;
        }));
        
        return res.json({
            success: true,
            ast: cleanAst
        });
    } catch (error) {
        console.error('Error parsing DSL:', error);
        return res.status(500).json({ 
            error: 'Internal server error',
            message: error instanceof Error ? error.message : String(error)
        });
    }
});

/**
 * Health check endpoint
 */
app.get('/health', (req, res) => {
    res.json({ status: 'ok', service: 'Simple DSL Parser' });
});

/**
 * Root endpoint with API information
 */
app.get('/', (req, res) => {
    res.json({
        name: 'Simple DSL Parser REST API',
        version: '1.0.0',
        endpoints: {
            '/health': 'GET - Health check',
            '/parse': 'POST - Parse DSL content and return AST (expects { "content": "..." })'
        }
    });
});

// Start the server
app.listen(port, () => {
    console.log(`Simple DSL Parser REST API listening on port ${port}`);
    console.log(`Try: POST http://localhost:${port}/parse`);
    console.log(`With body: { "content": "Hello World!" }`);
});
