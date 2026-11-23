# SUYZISS FEED — Web

Responsive React + TypeScript + Vite client for the SUYZISS FEED project. The Android app remains in the repository unchanged.

## Stack
- React + TypeScript + Vite
- Supabase Auth, PostgreSQL, Storage
- Row Level Security for profiles, follows, posts, likes, and media

## Setup
1. Create a Supabase project.
2. In Supabase SQL Editor, run `supabase/migrations/001_initial.sql`.
3. Copy `.env.example` to `.env` and set the project URL and publishable/anon key.
4. From this directory run `npm install` then `npm run dev`.

Only the public publishable/anon key belongs in the browser. Never put a service_role key in frontend code.

## Current implementation
- Email/password registration and login
- Profile editing and private-account setting
- Real feed, text/photo/video posts, likes
- Username search and follow requests/acceptance
- System/light/dark theme
- No seeded or mock posts

The private media bucket is intentionally not public. Media URLs are signed for authenticated viewers. Apply the SQL migration before using the application.
