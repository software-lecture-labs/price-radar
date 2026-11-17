/** Pair with `aria-invalid` and `aria-describedby={id}` on the input it describes. */
export function FieldError({ id, message }: { id: string; message?: string }) {
  if (!message) {
    return null;
  }
  return (
    <p id={id} className="text-sm text-destructive">
      {message}
    </p>
  );
}

/** Props that link an input to its {@link FieldError}. */
export function errorProps(id: string, message?: string) {
  return message ? { "aria-invalid": true, "aria-describedby": id } : {};
}
