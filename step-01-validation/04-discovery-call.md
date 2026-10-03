# Discovery call script (Step 1.3)

Length: 20 minutes. You talk 30%. They talk 70%. Do not share the UI unless they ask.

## Open (60 seconds)

“I am not selling you payroll. I am checking whether the day-before-payday Excel ritual is painful enough that a specialist checker is worth $299/month. I’ll ask eight questions. If it still looks real at the end, I’ll ask for a de-identified punch export.”

## Questions (ask in this order, write answers in `notes-template.md`)

1. What does the day before payroll look like, hour by hour?
2. Which mistakes have you actually made in the last year?
3. Which state(s) and cities are your clinics in?
4. How many pay rates can one person have in a single day?
5. Do you pay production / collections bonuses? How often? Do they get folded into overtime?
6. What time clock do you use today (Deputy, Homebase, TimeClock Plus, paper, Google Sheet)?
7. What payroll system (Gusto, ADP, Paychex, QuickBooks, Paycom)?
8. What would you pay monthly to never think about meal-break premiums again?

## Close

“Send a CSV with employee_code, timestamp, punch type. I’ll return a two-page autopsy: dollars at risk, premiums the payroll system never booked, and whether a second period is worth $150.”

## Stop rules

- Fewer than 3 people will send real punch data → do not start Step 2.
- They want an EHR, applicant tracking, or tax filing first → wrong buyer; thank them and leave.
- They are a single-location practice with 4 people and no hourly staff → too small.

## After the call

Copy this file to `step-01-validation/calls/YYYY-MM-DD-<clinic-slug>.md` and fill the template.
