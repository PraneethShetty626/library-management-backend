#!/bin/bash

# ---------------- Config ----------------
BASE_URL="http://localhost:8080"
ADMIN_USER="admin"
ADMIN_PASS="admin123"
USER_NAME="testuser"
USER_PASS="newPassword"

# ---------------- Helper Functions ----------------
function login() {
  local username=$1
  local password=$2
  echo "Logging in as $username..."
  TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\": \"$username\", \"password\": \"$password\"}" )
  echo "Token: $TOKEN"
}

function curl_auth() {
  # Wrapper for authenticated requests
  local method=$1
  local url=$2
  local data=$3

  if [ -z "$data" ]; then
    response=$(curl -s -w "\n%{http_code}" -X $method "$url" -H "Authorization: Bearer $TOKEN")
  else
    response=$(curl -s -w "\n%{http_code}" -X $method "$url" \
      -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d "$data")
  fi

  # Split body and status code
  http_body=$(echo "$response" | sed '$d')
  http_code=$(echo "$response" | tail -n1)

  echo "HTTP Status: $http_code"
  echo "Response Body: $http_body"
}


# ---------------- Admin Login ----------------
login $ADMIN_USER $ADMIN_PASS

echo
echo "------ ADMIN ACTIONS ------"

# Register a new user
echo "Registering new user..."
curl_auth POST "$BASE_URL/auth/register" '{"username":"'$USER_NAME'","password":"'$USER_PASS'","roles":["ROLE_USER"]}'
echo

# Get all users
#echo "Fetching all users..."
#curl_auth GET "$BASE_URL/users?page=0&size=10"
#echo

## Disable a user
#echo "Disabling user ID 2..."
#curl_auth PATCH "$BASE_URL/users/2/disable"
#echo
#
## Enable a user
#echo "Enabling user ID 2..."
#curl_auth PATCH "$BASE_URL/users/2/enable"
#echo

# Expire a user
#echo "Expiring user ID 2..."
#curl_auth PATCH "$BASE_URL/users/2/expire"
#echo

# Update user
#echo "Updating user ID 2..."
#curl_auth PUT "$BASE_URL/users/2/updateName?username=$USER_NAME"
#echo

#echo "Creating a new book..."
#curl_auth POST "$BASE_URL/api/books" '{"title":"New Book Java ","author":"Author X john ","isbn":"1234567890","available":true}'
#echo

echo "Getting all borrowed books..."
curl_auth GET "$BASE_URL/api/books/borrowed"
echo

#
# ---------------- Normal User Login ----------------
login $USER_NAME $USER_PASS
#
#echo
#echo "------ USER ACTIONS ------"
#
# Update own password
#echo "Updating own password..."
#curl_auth PATCH "$BASE_URL/users/2/password" '{"password":"'$USER_PASS'"}'
#echo

# Get all books
echo "Fetching all books..."
curl_auth GET "$BASE_URL/api/books?page=0&size=10"
echo

# Get available books
echo "Fetching available books..."
curl_auth GET "$BASE_URL/api/books/available?page=0&size=10"
echo
#
## Borrow a book
echo "Borrowing book ID 1..."
curl_auth POST "$BASE_URL/api/books/1/borrow" '{"id":3,"username":"testuser"}'
echo
#

#
## Search books by title
echo "Searching books by title 'Java'..."
curl_auth GET "$BASE_URL/api/books/title/Java?page=0&size=10"
echo
#
## Search books by author
echo "Searching books by author 'John'..."
curl_auth GET "$BASE_URL/api/books/author/John?page=0&size=10"
echo

# Return a book
echo "Getting all borrowed books..."
curl_auth GET "$BASE_URL/api/books/borrowed/2"
echo

echo "Getting all borrowed books..."
curl_auth GET "$BASE_URL/api/books/borrowed"
echo

# Return a book
echo "Returning book ID 1..."
curl_auth POST "$BASE_URL/api/books/1/return"
echo

#
## Admin book actions (requires re-login as ADMIN)
#
#
## ---------------- Print all users at the end ----------------
login $ADMIN_USER $ADMIN_PASS

echo "Getting all borrowed books..."
curl_auth GET "$BASE_URL/api/books/borrowed"
echo

echo "Getting all available books..."
curl_auth GET "$BASE_URL/api/books/available"
echo

#
#echo
#echo "------ FINAL LIST OF USERS ------"
#curl_auth GET "$BASE_URL/users?page=0&size=50"
#echo
