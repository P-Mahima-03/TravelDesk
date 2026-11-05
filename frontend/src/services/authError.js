export function getAuthErrorMessage(error) {
  const responseData = error?.response?.data;
  const validationMessages = Array.isArray(responseData?.errors)
    ? responseData.errors
        .map((item) =>
          typeof item === "string" ? item : item?.defaultMessage,
        )
        .filter((message) => typeof message === "string" && message.trim())
    : [];

  if (validationMessages.length > 0) {
    return validationMessages.join(" ");
  }

  const serverMessage =
    typeof responseData?.message === "string"
      ? responseData.message.trim()
      : typeof responseData?.detail === "string"
        ? responseData.detail.trim()
        : "";

  if (serverMessage) {
    return serverMessage;
  }

  if (!error?.response) {
    return "Unable to reach the server. Check your connection and try again.";
  }

  if (error.response.status === 401) {
    return "Invalid email or password.";
  }

  if (error.response.status === 409) {
    return "An account with this email already exists.";
  }

  if (error.response.status === 400) {
    return "Please check your details and try again.";
  }

  return "Something went wrong. Please try again.";
}
