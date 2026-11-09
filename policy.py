ALLOWED_APPS = {"youtube", "capcut"}
ALLOWED_MEDIA = {"play_pause", "next", "previous"}


def authorize(plan):
    if not isinstance(plan, dict):
        return "deny"

    if set(plan) != {"action", "args"}:
        return "deny"

    action = plan["action"]
    args = plan["args"]

    if not isinstance(action, str) or not isinstance(args, dict):
        return "deny"

    if action == "open_app":
        if set(args) != {"app"}:
            return "deny"
        return "allow" if args["app"] in ALLOWED_APPS else "deny"

    if action == "media_control":
        if set(args) != {"control"}:
            return "deny"
        return "allow" if args["control"] in ALLOWED_MEDIA else "deny"

    if action == "list_files":
        return "allow" if not args else "deny"

    return "deny"
