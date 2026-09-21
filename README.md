# MetroGO

MetroGO started as a UI prototype for a South African minibus/bus transit service and grew into a full end-to-end app: real account registration, a persistent wallet, a working ticket-purchase pipeline with QR code generation, journey planning across a small network of routes, and a data model that's been deliberately normalized in preparation for a move to a real cloud backend.

## Table of Contents

- [Purpose](#purpose)
- [Core Concepts](#core-concepts)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Data Model](#data-model)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Firebase Integration Status](#firebase-integration-status)
- [Roadmap](#roadmap)
- [Known Limitations](#known-limitations)

---

## Purpose

Public transit in the context MetroGO is built for is often cash-based, paper-ticketed, and hard to plan around. MetroGO's goal is to bring that experience in line with what the people in the community expect from modern day apps:

- **Know what's running** — browse real bus routes and departure times before you leave.
- **Pay once, ride many times** — the people will be able to top up their credit balance in app instead of going to a branch just to increases their credits they can do all this in the apps in-app wallet instead of carrying exact change.
- **Board with your phone** — they will be given a QR-coded digital ticket instead of a paper stub making it accessible and always with you , making you accountable and making it accessible aswell.
- **See where you've been** — a travel history with a lightweight rewards (XP/level) system to make regular riders feel recognized.

## Core Concepts

Core Concepts
A few simple ideas are used throughout the app. Understanding these first will make the rest of the code much easier to follow:

Each user has their own data. Everything in the app belongs to the person who is currently logged in. This includes their wallet balance, tickets, transaction history, and notifications. If another person logs in, they see their own information instead. In other words, one user cannot accidentally see another user's data.

A route, a departure, and a ticket are three different things. 
Seperated so that the developers can understand :
  A route is the journey itself, such as "T9 Vea Raya, Park Station → Soshanguve."
  A schedule is a specific trip on that route, such as "T9 leaves at 07:30 tomorrow."
  A ticket is someone's actual purchase for that particular trip.

Keeping these separate means we don't have to save the same route and departure information over and over again for every ticket. For example, hundreds of people can buy tickets for the same 07:30 trip, while the route and schedule information is stored only once. See Data Model.

The wallet keeps a proper history of money movements. The wallet isn't simply a number that gets changed whenever someone adds or spends money. Every top-up and every purchase is saved as a transaction. The app also records what the balance was after each transaction. This means the Wallet screen can show exactly how the balance changed over time, rather than trying to work it out afterwards.

The app works without needing the internet ,and it is equipped with online storage. The information that is stored and assigned values are organized in a structured way, similar to how a small database would be organized. This makes it easier to move the app  without having to completely redesign how the data works.

## Features

**Accounts & Profile**
- Registration and login with SHA-256-hashed passwords (never stored in plaintext)
- Session persists across app restarts — no need to log in every time
- Editable profile (name, mobile, ID number, date of birth) that actually persists
- Account deletion that fully cleans up a user's tickets, wallet, transactions, and notifications

**Ticketing**
- Browse available bus routes and departure times
- Purchase a ticket, paid for from the in-app wallet
- A real scannable QR code is generated per ticket (via ZXing), encoding trip and passenger details
- Ticket history with XP earned per purchase and a level/progress system

**Journey Planning**
- Pick From/To stations and see matching scheduled departures
- Swap origin/destination with one tap
- Also surfaces other available routes that don't match the search, so riders can see what else is running

**Wallet**
- Top up by preset amount or custom entry
- Full transaction history (top-ups and purchases) with running balance
- New accounts start at R0 — no fake starter balance

**Notifications**
- In-app notifications for ticket purchases and wallet top-ups
- Read/unread tracking

**Multi-language support**
- Live, on-device translation of app content via ML Kit Translate, with a per-user language preference

**Design**
- A cohesive, flat, professional visual language (white top bars, neutral card surfaces, real vector iconography) applied consistently across every screen

## Tech Stack

| Layer           | Choice |
| Language        | Kotlin |
| UI framework    | Android Views (XML layouts + `ConstraintLayout`), **not** Jetpack Compose |
| Components      | Material Components for Android (`MaterialButton`, etc.) |
| QR codes        | [ZXing](https://github.com/zxing/zxing) (`core`) — generated on-device, no network call needed |
| Translation     | ML Kit Translate — on-device, offline-capable |
| Cloud backend   | Firebase (Authentication + Firestore) — dependencies wired in and connectivity has been verified

## Data Model

The local data model is deliberately normalized to **Third Normal Form**, designed to map directly onto Firestore collections later without needing to redesign relationships:

| Entity               | Purpose |

| `UserAccount`        | A registered rider — name, email, hashed password, mobile, ID number, DOB, language preference |
| `BusStop`            | A physical stop/station, with coordinates |
| `TransportRoute`     | An abstract, reusable path between two stops (no time attached) |
| `Schedule`           | One concrete timed departure of a route (time, price, bus registration) |
| `Ticket`             | A purchase of a specific schedule by a specific user |
| `TransportCard`      | The in-app wallet — balance, card number, status |
| `Payment`            | A ledger entry for every top-up or purchase, with a running balance snapshot |
| `AppNotification`    | An in-app notification, with read/unread state |
| `TravelHistoryEntry` | A record of a trip taken, kept separate from the `Ticket` that paid for it |

A few fields (ticket price, payment amount, travel-history fare) are deliberately *not* fully normalized away — they're point-in-time snapshots of what was actually paid, which is standard invoicing practice, not an oversight.



## Known Limitations

- Route and schedule data is a small hand-authored sample set, not a live feed
- No real payment gateway — wallet top-ups are simulated, not processed through an actual payment provider
