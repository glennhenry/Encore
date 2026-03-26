package encoreTest

/**
 * Manual testing in server (ktor, routing, non-sockets)
 *
 * --core routing--
 * 1. Unhandled API/routes returns a custom HTML template with error code and message.
 * 2. Unhandled exception returns internal server error (500) with error trace in development mode.
 * 3. Unhandled exception returns internal server error (500) with flavour text in non-development mode.
 * --docs routing--
 * 4. In deployment mode, /docs return docs website.
 * 5. In non-deployment mode, /docs return 404.
 * --backstage--
 * 6. In development mode, /backstage bypass auth and return main.html
 * 7. In non-development mode, /backstage return wall.html
 * 8. /backstage login with token succeed
 * 9. /backstage login with cookie succeed
 * 10. /backstage login with cookie failed when expired (6 hours)
 * --backstage websocket--
 * 11. In non/development mode, /backstage websocket connects to logger, monitor, command
 * 12. /backstage logger, monitor, command work as expected
 */
